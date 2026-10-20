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
    @Autowired org.springframework.web.servlet.resource.ResourceUrlProvider assetUrls;
    @Autowired DemoPreparationService preparation;
    @Autowired ParticipantService participants;
    @Autowired ParticipantRepository people;
    @Autowired MatchService matches;
    @Autowired MatchCompletionSagaSteps completionSteps;
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
    @Autowired com.ping.interest.InterestService interests;
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
    long complete() { long m = matching.search(a); matches.accept(m,a); matches.accept(m,b); matches.arrive(m,a); matches.arrive(m,b); matches.confirm(m,a); matches.confirm(m,b); return m; }
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
            matches.arrive(m,a); matches.arrive(m,b); matches.confirm(m,a); matches.confirm(m,b);
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
            assertThat(page.body()).contains("Zona Gamer · Pabellón 18", "aria-current=\"location\"", "href=\"/matches/"+m+"\"", assetUrls.getForLookupPath("/js/event-map.js"));
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
            assertThat(browser.get(path).body()).doesNotContain("Tu encuentro con", "Tu punto de encuentro", "Volver al encuentro");
            matches.accept(m,a); matches.accept(m,b); assertThat(browser.get(path).body()).contains("Volver al encuentro");
            EventForm f = new EventForm(); f.setName("Otro evento"); long other = events.create(f).getId(); events.transition(other,EventStatus.OPEN);
            ParticipantForm p = new ParticipantForm(); p.setAlias("Fuera de SOFA"); p.setEventId(other); long outside = participants.create(p).id();
            enter(browser,outside); assertThat(browser.get(path).body()).contains("Entrar al evento").doesNotContain("Hola, Fuera de SOFA.");
        }
    }
    @Test void hubKeepsGuestExplorationAvailabilityVersionAndRealBalance() throws Exception {
        String path = "/events/"+event+"/hub";
        try(var browser = new TestBrowser(port)) {
            var guest = browser.get(path);
            assertThat(guest.statusCode()).isEqualTo(200);
            assertThat(guest.body()).contains("Entrar al evento", "Encuentra tu mundo", "Sin vínculo oficial", assetUrls.getForLookupPath("/img/sofa/convencion-social.webp"))
                .doesNotContain("data-meeting=\"true\"", "Volver al encuentro", "action=\"/me/search\"");
            enter(browser,a);
            AvailabilityForm f = new AvailabilityForm(); f.setVersion(participants.get(a).version()); f.setAvailable(false); participants.setAvailability(a,f);
            String idle = browser.get(path).body();
            assertThat(idle).contains("Estoy listo para conectar", "action=\"/me/availability\"", "name=\"_csrf\"", "name=\"version\"", "value=\""+participants.get(a).version()+"\"")
                .doesNotContain("action=\"/me/search\"");
            // Exercise the real form contract; an outdated version must still be rejected.
            assertThat(browser.post("/me/availability",Map.of("available","true","version",Long.toString(participants.get(a).version()-1))).statusCode()).isEqualTo(409);
            assertThat(participants.get(a).available()).isFalse();
            browser.get(path);
            assertThat(browser.post("/me/availability",Map.of("available","true","version",Long.toString(participants.get(a).version()))).statusCode()).isEqualTo(302);
            assertThat(browser.get(path).body()).contains("Buscar conexión");
            complete();
            assertThat(browser.get(path).body()).contains("10 PTS").doesNotContain(">0 PTS<", "Tu punto de encuentro");
        }
    }
    @ParameterizedTest @CsvSource({"PAUSED", "CLOSED"})
    void hubRespectsEventStateWithoutBlockingAnExistingActiveEncounter(EventStatus status) throws Exception {
        long m = matching.search(a); matches.accept(m,a); matches.accept(m,b);
        events.transition(event,status);
        try(var browser = new TestBrowser(port)) {
            enter(browser,a);
            assertThat(browser.get("/events/"+event+"/hub").body()).contains("Volver al encuentro", "data-status=\""+status+"\"", "Tu punto de encuentro")
                .doesNotContain("action=\"/me/search\"", "action=\"/me/availability\"");
            var carlos = preparation.current().people().get(3).id(); enter(browser,carlos);
            assertThat(browser.get("/events/"+event+"/hub").body()).contains("Sin nuevas conexiones", "Abrir mapa")
                .doesNotContain("action=\"/me/search\"", "action=\"/me/availability\"", "Tu punto de encuentro");
        }
    }
    @ParameterizedTest @CsvSource({"Videojuegos,gamer", "Tecnología,technology", "Música,stage", "Fotografía,cosplay", "Anime,communities", "Cine,meet"})
    void hubHighlightsOnlyTheActiveZoneByIdEvenWhenPavilionsAreShared(String interest,String zone) throws Exception {
        for(long id : List.of(a,b)) {
            var selection = interests.selection(id);
            selection.setInterestIds(interests.catalog().stream().filter(i -> i.getName().equals(interest)).map(com.ping.interest.Interest::getId).toList());
            interests.save(id,selection);
        }
        long m = matching.search(a);
        try(var browser = new TestBrowser(port)) {
            enter(browser,a);
            matches.accept(m,a);
            assertThat(browser.get("/events/"+event+"/hub").body()).contains("Ver propuesta").doesNotContain("Tu punto de encuentro");
            matches.accept(m,b);
            var response = browser.get("/events/"+event+"/hub");
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("data-zone=\""+zone+"\" data-meeting=\"true\"", "zone="+zone+"&amp;matchId="+m, "Tu encuentro con", "Julián")
                .doesNotContain("Gorra roja", "Camiseta negra y morral", "data-other-hint");
            assertThat(response.body().split("class=\"h-here\"",-1)).hasSize(2);
            for(var z : EventExperienceCatalog.forEvent("SOFA 2026").zones()) {
                assertThat(response.body()).contains("data-zone=\""+z.id()+"\" data-meeting=\""+z.id().equals(zone)+"\"");
            }
        }
    }
    @Test void hubWithAnUncataloguedPointKeepsTheEncounterWithoutInventingAZone() throws Exception {
        long m = matching.search(a); matches.accept(m,a); matches.accept(m,b);
        // Historical/unrecognised presentation point: never infer a zone from a pavilion substring.
        sql.update("update ping_matches set meeting_point = ? where id = ?", "Punto anterior · Pabellón 17", m);
        try(var browser = new TestBrowser(port)) {
            enter(browser,a); var page = browser.get("/events/"+event+"/hub");
            assertThat(page.statusCode()).isEqualTo(200);
            assertThat(page.body()).contains("Volver al encuentro", "Punto anterior · Pabellón 17")
                .doesNotContain("data-meeting=\"true\"", "Tu punto de encuentro", "Gorra roja");
        }
    }
    @Test void activeMissionUsesRealSessionFormsAndKeepsUnilateralConfirmationDistinct() throws Exception {
        long m = matching.search(a); String path = "/matches/"+m;
        try(var sam = new TestBrowser(port); var jul = new TestBrowser(port)) {
            enter(sam,a); enter(jul,b);
            assertThat(sam.get(path).body()).contains("Aceptar conexión").doesNotContain("am-grid", "Gorra roja", assetUrls.getForLookupPath("/js/active-map.js"));
            assertThat(sam.post(path+"/accept",Map.of()).statusCode()).isEqualTo(302);
            assertThat(sam.get(path).body()).doesNotContain("am-grid", "Gorra roja");
            jul.get(path); assertThat(jul.post(path+"/accept",Map.of()).statusCode()).isEqualTo(302);
            sam.post(path+"/arrive",Map.of()); jul.post(path+"/arrive",Map.of());
            var active = matches.get(m,a);
            assertThat(sam.get(path).body()).contains("class=\"am-grid\"", "Su palabra de encuentro", active.word(), active.meetingPoint(), active.activity(), "Gorra roja", "Confirmaciones: 0 de 2", assetUrls.getForLookupPath("/js/active-map.js"));
            assertThat(sam.post(path+"/confirm",Map.of()).statusCode()).isEqualTo(302);
            assertThat(sam.get(path).body()).contains("Tú ya confirmaste. Esperando a Julián", "Confirmaciones: 1 de 2")
                .doesNotContain("action=\""+path+"/confirm\"", "Conexión completada");
            assertThat(jul.get(path).body()).contains("Samuel ya confirmó. Falta tu confirmación.", "Confirmar encuentro y actividad");
            assertThat(participants.get(a).points()).isZero(); assertThat(participants.get(b).points()).isZero();
            assertThat(matches.get(m,b).word()).isEqualTo(active.word());
            assertThat(matches.get(m,b).meetingPoint()).isEqualTo(active.meetingPoint());
            jul.post(path+"/confirm",Map.of()); sam.post(path+"/confirm",Map.of()); jul.post(path+"/confirm",Map.of());
            assertThat(participants.get(a).points()).isEqualTo(10); assertThat(participants.get(b).points()).isEqualTo(10);
            assertThat(sam.get(path).body()).contains("Conexión completada", "Feedback opcional")
                .doesNotContain("am-grid", assetUrls.getForLookupPath("/js/active-map.js"), "Gorra roja");
        }
    }
    @Test void activeMissionRetainsDurablePendingSagaAndRetryWithoutPrematureCompletion() throws Exception {
        long m = matching.search(a); matches.accept(m,a); matches.accept(m,b);
        matches.arrive(m,a); matches.arrive(m,b);
        completionSteps.begin(m,a); completionSteps.begin(m,b); // Durable state left before the reward steps.
        try(var browser = new TestBrowser(port)) {
            enter(browser,a); String path = "/matches/"+m;
            assertThat(browser.get(path).body()).contains("Finalizando la conexión", "Reintentar finalización", "Confirmaciones: 2 de 2")
                .doesNotContain("Conexión completada", "action=\""+path+"/cancel\"");
            assertThat(participants.get(a).points()).isZero(); assertThat(participants.get(b).points()).isZero();
            assertThat(browser.post(path+"/confirm",Map.of()).statusCode()).isEqualTo(302);
            browser.post(path+"/confirm",Map.of());
            assertThat(matches.get(m,a).status()).isEqualTo(MatchStatus.COMPLETED);
            assertThat(participants.get(a).points()).isEqualTo(10); assertThat(participants.get(b).points()).isEqualTo(10);
        }
    }
    @ParameterizedTest @CsvSource({"Tecnología,technology", "Música,stage"})
    void inlineEncounterMapKeepsTheAssignedZoneIdAndMatchReturn(String interest,String zone) throws Exception {
        for(long id : List.of(a,b)) {
            var form = interests.selection(id);
            form.setInterestIds(interests.catalog().stream().filter(i -> i.getName().equals(interest)).map(com.ping.interest.Interest::getId).toList()); interests.save(id,form);
        }
        long m = matching.search(a); matches.accept(m,a); matches.accept(m,b);
        try(var browser = new TestBrowser(port)) {
            enter(browser,a); String body = browser.get("/matches/"+m).body();
            assertThat(body).contains("data-zone-id=\""+zone+"\"", "zone="+zone+"&amp;matchId="+m, "data-encounter-zoom=\"in\"", "data-encounter-center", "aria-current=\"location\"");
            assertThat(body.split("data-assigned=\"true\"",-1)).hasSize(2);
            assertThat(browser.get("/events/"+event+"/map?zone="+zone+"&matchId="+m).body()).contains("href=\"/matches/"+m+"\"", "Volver al encuentro");
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
            // A completed pair cannot repeat: the next badge comes from a distinct partner.
            var newPartner=preparation.prepare().people().stream().filter(p->p.id()!=a&&p.id()!=b).findFirst().orElseThrow();
            AvailabilityForm off=new AvailabilityForm();off.setVersion(participants.get(b).version());off.setAvailable(false);participants.setAvailability(b,off);b=newPartner.id();available(b);
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
        matches.arrive(m,ana); matches.arrive(m,carlos); matches.confirm(m,ana); matches.confirm(m,carlos);
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
