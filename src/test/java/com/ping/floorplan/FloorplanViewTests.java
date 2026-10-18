package com.ping.floorplan;

import java.util.*;
import com.ping.encounter.*;import com.ping.event.*;import com.ping.match.*;import com.ping.participant.*;import com.ping.support.TestBrowser;
import org.junit.jupiter.api.*;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.test.context.SpringBootTest;import org.springframework.boot.test.web.server.LocalServerPort;import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties="spring.datasource.url=jdbc:h2:mem:floorplan-view;MODE=MySQL;DB_CLOSE_DELAY=-1") @ActiveProfiles("test")
class FloorplanViewTests {
    @Autowired EventService events;@Autowired EncounterService encounters;@Autowired FloorplanService plans;@Autowired ParticipantService people;@Autowired MatchingService matching;@Autowired MatchService matches;
    @LocalServerPort int port;
    long event,point;
    @BeforeEach void setup(){var e=new EventForm();e.setName("Evento genérico sin SOFA");event=events.create(e).getId();events.transition(event,EventStatus.OPEN);var p=new PointForm();p.setName("Punto original");p.setDirections("Indicaciones originales");point=encounters.savePoint(event,null,p);var a=new ActivityForm();a.setTitle("Conversar");a.setInstructions("Compartan una recomendación.");a.setPointIds(Set.of(point));encounters.saveActivity(event,null,a);}
    long person(String alias){var f=new ParticipantForm();f.setAlias(alias);f.setEventId(event);f.setAvailable(true);return people.create(f).id();}
    void enter(TestBrowser browser,long person)throws Exception{browser.get("/demo");assertThat(browser.post("/demo",Map.of("participantId",""+person)).statusCode()).isEqualTo(302);}
    @Test void assignmentRequiresOwnSessionAndBothAcceptancesAndFreezesResourceAcrossReplacement()throws Exception{
        String first=plans.upload(event,null,false,FloorplanTests.image("png",400,300));plans.place(event,first,0,point,.25,.75);
        long a=person("Dueño"),b=person("Conexión"),other=person("Otra sesión");long match=matching.search(a);
        try(var own=new TestBrowser(port);var stranger=new TestBrowser(port)){
            enter(own,a);enter(stranger,other);assertThat(own.get("/matches/"+match+"/plan").statusCode()).isEqualTo(409);assertThat(stranger.get("/matches/"+match+"/plan").statusCode()).isEqualTo(404);
            matches.accept(match,a);matches.accept(match,b);plans.place(event,first,1,point,.8,.2);
            String next=plans.upload(event,first,true,FloorplanTests.image("png",500,300));var changed=encounters.pointForm(event,point);changed.setName("Punto cambiado");changed.setDirections("Indicaciones cambiadas");encounters.savePoint(event,point,changed);
            var response=own.get("/matches/"+match+"/plan");assertThat(response.statusCode()).isEqualTo(200);assertThat(response.body()).contains(first,"Punto original","Indicaciones originales").doesNotContain(next,"Punto cambiado","Indicaciones cambiadas");
            var viaEvent=own.get("/events/"+event+"/plan");assertThat(viaEvent.body()).contains(first,"Punto original").doesNotContain(next);assertThat(own.get("/me/event").headers().firstValue("Location").orElseThrow()).endsWith("/events/"+event+"/plan");
        }
    }
    @Test void genericWithoutPlanOrMarkerKeepsDirectionsWithoutSofaFallback()throws Exception{
        try(var b=new TestBrowser(port)){
            var empty=b.get("/events/"+event+"/plan");assertThat(empty.statusCode()).isEqualTo(200);assertThat(empty.body()).contains("No hay un plano disponible","Punto original","Indicaciones originales").doesNotContain("/floorplan/images/","Plano esquemático");
            String id=plans.upload(event,null,false,FloorplanTests.image("png",400,300));var noMarker=b.get("/events/"+event+"/plan");assertThat(noMarker.body()).contains(id,"El punto no está ubicado","Indicaciones originales");
            assertThat(b.get("/events/"+event+"/plan?pointId=999999").statusCode()).isEqualTo(404);
            long a=person("Sin marcador"),other=person("Conexión sin marcador"),match=matching.search(a);matches.accept(match,a);matches.accept(match,other);enter(b,a);
            assertThat(b.get("/matches/"+match).body()).contains("Ver plano · Punto sin ubicar");assertThat(b.get("/matches/"+match+"/plan").body()).contains("El punto no está ubicado","Indicaciones originales");
        }
    }
    @Test void previewWorksWithAnImageAndNoMeetingPoints()throws Exception{
        var e=new EventForm();e.setName("Solo imagen");long empty=events.create(e).getId();String id=plans.upload(empty,null,false,FloorplanTests.image("png",400,300));
        try(var b=new TestBrowser(port)){assertThat(b.get("/events/"+empty+"/floorplan").body()).contains("/floorplan/images/"+id,"Crea primero un punto");}
    }
}
