package com.ping.floorplan;

import java.util.*;
import com.ping.encounter.*;import com.ping.event.*;import com.ping.match.*;import com.ping.participant.*;import com.ping.support.TestBrowser;
import org.junit.jupiter.api.*;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.test.context.SpringBootTest;import org.springframework.boot.test.web.server.LocalServerPort;import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties="spring.datasource.url=jdbc:h2:mem:event-activity-guide;MODE=MySQL;DB_CLOSE_DELAY=-1") @ActiveProfiles("test")
class EventActivityGuideTests {
    @Autowired EventService events;@Autowired EncounterService encounters;@Autowired ParticipantService people;@Autowired MatchingService matching;@Autowired MatchService matches;
    @LocalServerPort int port;
    long event,other;
    long open(String name){var e=new EventForm();e.setName(name);long id=events.create(e).getId();events.transition(id,EventStatus.OPEN);return id;}
    long point(long event,String name){var p=new PointForm();p.setName(name);p.setDirections("Junto al letrero de "+name);return encounters.savePoint(event,null,p);}
    long activity(long event,long point,String title,String instructions,int reward){var a=new ActivityForm();a.setTitle(title);a.setInstructions(instructions);a.setRewardPoints(reward);a.setPointIds(Set.of(point));return encounters.saveActivity(event,null,a);}
    long person(long event,String alias){var f=new ParticipantForm();f.setAlias(alias);f.setEventId(event);f.setAvailable(true);return people.create(f).id();}
    void enter(TestBrowser browser,long person)throws Exception{browser.get("/demo");assertThat(browser.post("/demo",Map.of("participantId",""+person)).statusCode()).isEqualTo(302);}
    static String pts(int points){return "<strong>+"+points+"</strong>";}
    static int count(String body,String text){return body.split(java.util.regex.Pattern.quote(text),-1).length-1;}
    @BeforeEach void setup(){
        event=open("Feria USTA de prueba");long p=point(event,"Plazoleta");
        activity(event,p,"Recomienden un libro","Cada persona recomienda un libro y explica por qué.",15);
        activity(event,p,"Pregunta rápida","Respondan juntos una pregunta del cartel.",25);
        activity(event,p,"Mapa de intereses","Encuentren tres intereses en común.",40);
        long off=activity(event,p,"Actividad pausada","No debe verse.",5);var f=encounters.activityForm(event,off);f.setActive(false);encounters.saveActivity(event,off,f);
        encounters.archiveActivity(event,activity(event,p,"Actividad archivada","Tampoco debe verse.",5));
        other=open("Otro evento aislado");activity(other,point(other,"Lobby"),"Actividad de otro evento","Solo pertenece al otro evento.",70);
    }
    @Test void eventTabListsOnlyThisEventsEnabledActivitiesBeforeThePlan()throws Exception{
        long me=person(event,"Exploradora");
        try(var b=new TestBrowser(port)){
            enter(b,me);assertThat(b.get("/me/event").headers().firstValue("Location").orElseThrow()).endsWith("/events/"+event+"/plan");
            String body=b.get("/events/"+event+"/plan").body();
            assertThat(body).contains("Actividades y puntos","3 actividades · de +15 a +40 puntos","Recomienden un libro","Cada persona recomienda un libro y explica por qué.",pts(15),"Pregunta rápida",pts(25),"Mapa de intereses",pts(40),"Cada actividad suma sus puntos a cada persona, una sola vez, cuando ambos confirman la actividad.","se verifica y confirma desde tu encuentro","Ir al plano del evento","Plazoleta","Junto al letrero de Plazoleta")
                .doesNotContain("Actividad pausada","Actividad archivada","Actividad de otro evento",pts(70),"Tu actividad asignada",pts(10),"Aún no hay actividades");
            assertThat(body.indexOf("Actividades y puntos")).isLessThan(body.indexOf("id=\"plano\""));
            assertThat(body.indexOf("Mapa de intereses")).isLessThan(body.indexOf("Cada actividad suma sus puntos"));
            assertThat(body).doesNotContainPattern("<form[^>]*/(matches|activities)[^>]*>");
        }
    }
    @Test void activeEncounterHighlightsAssignedActivityOnceAndKeepsDirectionsAndReturn()throws Exception{
        long a=person(event,"Ana"),b2=person(event,"Beto");long match=matching.search(a);matches.accept(match,a);matches.accept(match,b2);
        var view=matches.get(match,a);assertThat(view.activityTitle()).isEqualTo("Recomienden un libro");
        try(var b=new TestBrowser(port)){
            enter(b,a);String body=b.get("/events/"+event+"/plan").body();
            assertThat(body).contains("Tu punto de encuentro","Ver todas las actividades · 3","href=\"#actividades\"","Tu actividad y 2 más · de +15 a +40 puntos","Tu actividad asignada","Se verifica y confirma desde tu encuentro","Otras actividades del evento","Pregunta rápida","Mapa de intereses","Volver al encuentro","/matches/"+match,"Junto al letrero de Plazoleta")
                .doesNotContain("Actividad de otro evento","Actividad pausada");
            assertThat(count(body,"Recomienden un libro")).isEqualTo(1);assertThat(count(body,pts(15))).isEqualTo(1);
            assertThat(body.indexOf("Ver todas las actividades")).isLessThan(body.indexOf("Junto al letrero de Plazoleta"));
            assertThat(body.indexOf("Junto al letrero de Plazoleta")).isLessThan(body.indexOf("Tu actividad asignada"));
            assertThat(b.get("/matches/"+match+"/plan").body()).contains("Tu actividad asignada","Volver al encuentro");
        }
    }
    @Test void emptyCatalogKeepsThePlanAndLegacyCatalogExplainsItsCommonReward()throws Exception{
        long empty=open("Evento sin actividades");point(empty,"Entrada");long me=person(empty,"Sin catálogo");
        try(var b=new TestBrowser(port)){
            enter(b,me);String body=b.get("/events/"+empty+"/plan").body();
            assertThat(body).contains("Aún no hay actividades disponibles para este evento","id=\"plano\"","Entrada","No hay un plano disponible").doesNotContain("Recomienden un libro","Actividad de otro evento");
        }
        var f=new EventForm();f.setName("Evento con catálogo anterior");f.setMeetingPoints("Patio");f.setEncounterActivities("Compartan una recomendación.\nElijan una pregunta para conversar.");long legacy=events.create(f).getId();events.transition(legacy,EventStatus.OPEN);long me2=person(legacy,"Anterior");
        try(var b=new TestBrowser(port)){
            enter(b,me2);String body=b.get("/events/"+legacy+"/plan").body();
            assertThat(body).contains("2 actividades · +"+EncounterService.LEGACY_REWARD_POINTS+" puntos cada una","Todas las actividades de este evento suman +"+EncounterService.LEGACY_REWARD_POINTS+" puntos","Compartan una recomendación.","Elijan una pregunta para conversar.").doesNotContain("Recomienden un libro","Zona Gamer");
            assertThat(count(body,pts(EncounterService.LEGACY_REWARD_POINTS))).isEqualTo(2);
        }
    }
}
