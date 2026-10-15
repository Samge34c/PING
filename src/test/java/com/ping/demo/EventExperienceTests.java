package com.ping.demo;

import java.util.*;
import com.ping.event.*;
import com.ping.experience.*;
import com.ping.match.*;
import com.ping.participant.*;
import com.ping.reward.*;
import com.ping.support.TestBrowser;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "spring.datasource.url=jdbc:h2:mem:event-experience;MODE=MySQL;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class EventExperienceTests {
    @Autowired DemoPreparationService preparation;
    @Autowired ParticipantService participants;
    @Autowired ParticipantRepository people;
    @Autowired MatchService matches;
    @Autowired MatchingService matching;
    @Autowired MatchRepository matchRepository;
    @Autowired MatchReservationRepository reservations;
    @Autowired FeedbackRepository feedback;
    @Autowired EventService events;
    @Autowired EventRepository eventRepository;
    @Autowired RewardService rewards;
    @Autowired RewardClaimRepository claims;
    @Autowired DemoRecognitionHints hints;
    @Autowired RecognitionHintService personalHints;
    @Autowired JdbcTemplate sql;
    @LocalServerPort int port;
    long a,b,event;
    @BeforeEach void setup() {
        sql.update("update demo_scenarios set event_id = null");
        claims.deleteAll(); feedback.deleteAll(); reservations.deleteAll(); matchRepository.deleteAll(); people.deleteAll(); eventRepository.deleteAll();
        var s = preparation.prepare(); event = s.eventId(); a = s.people().get(0).id(); b = s.people().get(1).id();
        available(a); available(b);
        for(var entry : Map.of(a,"Camiseta negra y morral",b,"Gorra roja").entrySet()) {
            var form = personalHints.ownForm(entry.getKey()); form.setHint(entry.getValue()); personalHints.save(entry.getKey(),form);
        }
    }
    void available(long id) {
        AvailabilityForm f = new AvailabilityForm(); f.setVersion(participants.get(id).version()); f.setAvailable(true); participants.setAvailability(id,f);
    }
    void enter(TestBrowser browser,long id) throws Exception { browser.get("/demo"); browser.post("/demo",Map.of("participantId",Long.toString(id))); }
    long complete() { long m = matching.search(a); matches.accept(m,a); matches.accept(m,b); matches.confirm(m,a); matches.confirm(m,b); return m; }
    @Test void recognitionIsOnlyVisibleDuringActiveMatchAndNeverInPublicListsOrHistory() throws Exception {
        long m = matching.search(a);
        try(var sam = new TestBrowser(port); var jul = new TestBrowser(port)) {
            enter(sam,a); enter(jul,b);
            for(String path:List.of("/matches/"+m,"/me")) assertThat(sam.get(path).body()).doesNotContain("Gorra roja","PISTA PARA ENCONTRARSE");
            matches.accept(m,a); assertThat(sam.get("/matches/"+m).body()).doesNotContain("Gorra roja");
            matches.accept(m,b);
            assertThat(sam.get("/matches/"+m).body()).contains("Gorra roja","Cómo reconocer a Julián","Abrir mapa");
            assertThat(jul.get("/matches/"+m).body()).contains("Camiseta negra y morral");
            assertThat(sam.get("/me").body()).doesNotContain("Gorra roja");
            for(String path:List.of("/participants","/admin","/events/"+event+"/hub","/events/"+event+"/map"))
                assertThat(sam.get(path).body()).doesNotContain("Gorra roja","Camiseta negra y morral","Tote bag y camiseta de anime");
            matches.confirm(m,a); matches.confirm(m,b);
            assertThat(sam.get("/matches/"+m).body()).doesNotContain("Gorra roja","PISTA PARA ENCONTRARSE");
            assertThat(sam.get("/me").body()).doesNotContain("Gorra roja");
        }
    }
    @Test void mapKeepsTheCorrectZoneAndRejectsInvalidOrForeignMatchLinks() throws Exception {
        long m = matching.search(a); matches.accept(m,a); matches.accept(m,b);
        try(var browser = new TestBrowser(port)) {
            enter(browser,a);
            String path = "/events/"+event+"/map?zone=gamer&matchId="+m;
            var page = browser.get(path); assertThat(page.statusCode()).isEqualTo(200);
            assertThat(page.body()).contains("Zona Gamer · Pabellón 18", "aria-current=\"location\"", "href=\"/matches/"+m+"\"", "/js/event-map.js");
            assertThat(browser.get("/events/"+event+"/map?zone=unknown").statusCode()).isEqualTo(404);
            var scenario = preparation.current(); enter(browser,scenario.people().get(2).id());
            assertThat(browser.get(path).statusCode()).isEqualTo(404);
            EventForm f = new EventForm(); f.setName("Otro evento"); long other = events.create(f).getId();
            assertThat(browser.get("/events/"+other+"/map").statusCode()).isEqualTo(404);
            assertThat(participants.get(a).points()).isZero();
        }
    }
    @Test void hubShowsPersonalNextActionAndDoesNotAdoptAnotherEventsParticipant() throws Exception {
        try(var browser = new TestBrowser(port)) {
            enter(browser,a); String path = "/events/"+event+"/hub";
            assertThat(browser.get(path).body()).contains("Corferias · Bogotá", "Hola, Samuel.", "Buscar conexión", "0 PTS", "SIMULACIÓN ACADÉMICA");
            long m = matching.search(a);
            assertThat(browser.get(path).body()).contains("Ver propuesta").doesNotContain("action=\"/me/search\"");
            matches.accept(m,a); matches.accept(m,b); assertThat(browser.get(path).body()).contains("Volver al encuentro");
            EventForm f = new EventForm(); f.setName("Otro evento"); long other = events.create(f).getId(); events.transition(other,EventStatus.OPEN);
            ParticipantForm p = new ParticipantForm(); p.setAlias("Fuera de SOFA"); p.setEventId(other); long outside = participants.create(p).id();
            enter(browser,outside); assertThat(browser.get(path).body()).contains("Entrar al evento").doesNotContain("Hola, Fuera de SOFA.");
        }
    }
    @Test void fictionalPromotionAndDerivedBadgesNeverAffectTheRealSnackBalance() throws Exception {
        try(var browser = new TestBrowser(port)) {
            enter(browser,a);
            assertThat(browser.get("/me/benefits").body()).contains("PROMOCIÓN DEMO", "NO VÁLIDO FUERA DE ESTA DEMO").doesNotContain("PING-GAMER");
            assertThat(participants.get(a).points()).isZero(); assertThat(claims.count()).isZero();
            long m = complete();
            assertThat(browser.get("/matches/"+m).body()).contains("Primer Ping", "SOFA Link", "Hay una promoción demo disponible");
            for(int i=0;i<2;i++) assertThat(browser.get("/me/benefits").body()).contains("PING-GAMER", "No consume puntos.", "NO VÁLIDO FUERA DE ESTA DEMO");
            assertThat(participants.get(a).points()).isEqualTo(10); assertThat(claims.count()).isZero();
            complete(); var history = matches.list(a);
            assertThat(EventExperienceCatalog.badges(history)).containsExactly("Primer Ping","Conector","SOFA Link");
            long benefit = rewards.catalog().getFirst().getId(); String key = UUID.randomUUID().toString();
            rewards.request(a,benefit,key); rewards.request(a,benefit,key);
            assertThat(participants.get(a).points()).isZero(); assertThat(claims.count()).isEqualTo(1);
            assertThat(browser.get("/me/benefits").body()).contains("PING-GAMER"); assertThat(participants.get(a).points()).isZero();
        }
    }
    @Test void recognitionFixturesAreNotAttachedToAnUnpreparedSofaEvent() {
        EventForm f = new EventForm(); f.setName("SOFA 2026"); long other = events.create(f).getId(); events.transition(other,EventStatus.OPEN);
        ParticipantForm p = new ParticipantForm(); p.setEventId(other); p.setAlias("Samuel"); long s = participants.create(p).id();
        p.setAlias("Julián"); long j = participants.create(p).id(); available(s); available(j);
        long m = matching.search(s); matches.accept(m,s); matches.accept(m,j);
        assertThat(hints.forMatch(other,s,matches.get(m,s))).isNull();
    }
    @Test void coincidingAliasOutsideTheFourFixturesDoesNotReceiveAClothingHint() {
        AvailabilityForm unavailable = new AvailabilityForm(); unavailable.setVersion(participants.get(b).version()); unavailable.setAvailable(false); participants.setAvailability(b, unavailable);
        ParticipantForm f = new ParticipantForm(); f.setEventId(event); f.setAlias("Julián"); long another = participants.create(f).id(); available(another);
        long m = matching.search(a); matches.accept(m,a); matches.accept(m,another);
        assertThat(hints.forMatch(event,a,matches.get(m,a))).isNull();
    }
    @Test void explorerBadgeRequiresACompletedAlternative() {
        var scenario = preparation.current();
        for(long id : List.of(a,b)) {
            AvailabilityForm f = new AvailabilityForm(); f.setVersion(participants.get(id).version()); f.setAvailable(false); participants.setAvailability(id,f);
        }
        long ana = scenario.people().get(2).id(), carlos = scenario.people().get(3).id(); available(ana); available(carlos);
        long m = matching.search(ana); matches.accept(m,ana); matches.accept(m,carlos);
        assertThat(EventExperienceCatalog.badges(matches.list(ana))).isEmpty();
        matches.confirm(m,ana); matches.confirm(m,carlos);
        assertThat(EventExperienceCatalog.badges(matches.list(ana))).containsExactly("Primer Ping","Explorador","SOFA Link");
    }
    @ParameterizedTest @CsvSource({"Videojuegos,gamer,18", "Tecnología,technology,17", "Anime,communities,3", "Fotografía,cosplay,8", "Música,stage,17", "Cine,meet,23", "'',meet,23"})
    void everyMeetingPointSelectsItsConfiguredMapZone(String interest,String zone,String pavilion) {
        var experience = EventExperienceCatalog.forEvent("SOFA 2026");
        var selected = experience.atPoint(ActivityCatalog.meetingPoint(interest));
        assertThat(selected.id()).isEqualTo(zone); assertThat(selected.pavilion()).isEqualTo(pavilion);
        assertThat(experience.markers()).extracting(EventExperienceCatalog.Zone::pavilion).doesNotHaveDuplicates();
    }
}
