package com.ping.demo;

import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import com.ping.admin.*;
import com.ping.event.*;
import com.ping.match.*;
import com.ping.participant.*;
import com.ping.support.TestBrowser;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:demo-improvements;MODE=MySQL;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class DemoImprovementTests {
    @Autowired DemoPreparationService preparation;
    @Autowired AdminService admin;
    @Autowired EventRepository events;
    @Autowired EventService eventService;
    @Autowired ParticipantRepository people;
    @Autowired ParticipantService participants;
    @Autowired MatchRepository matches;
    @Autowired MatchService matchService;
    @Autowired MatchingService matching;
    @Autowired MatchReservationRepository reservations;
    @Autowired FeedbackRepository feedback;
    @Autowired JdbcTemplate sql;
    @LocalServerPort int port;
    @BeforeEach void clear() {
        sql.update("update demo_scenarios set event_id = null");
        feedback.deleteAll(); reservations.deleteAll(); matches.deleteAll(); people.deleteAll(); events.deleteAll();
    }
    long id(DemoPreparationService.Scenario s, String alias) { return s.people().stream().filter(p -> p.alias().equals(alias)).findFirst().orElseThrow().id(); }
    void available(long id, boolean value) {
        AvailabilityForm f = new AvailabilityForm(); f.setAvailable(value); f.setVersion(participants.get(id).version()); participants.setAvailability(id, f);
    }
    EventMetrics metrics(long id) { return admin.metrics().stream().filter(e -> e.id() == id).findFirst().orElseThrow(); }
    @Test void preparationIsManualRepeatableAndDoesNotDeleteOtherData() {
        assertThat(preparation.current()).isNull(); assertThat(events.count()).isZero();
        EventForm f = new EventForm(); f.setName("Mi evento existente"); var own = eventService.create(f);
        var first = preparation.prepare(); var second = preparation.prepare();
        assertThat(first.eventId()).isEqualTo(second.eventId()); assertThat(first.status()).isEqualTo(EventStatus.OPEN);
        assertThat(first.people()).extracting(DemoPreparationService.Person::alias).containsExactly("Samuel", "Julián", "Ana", "Carlos");
        assertThat(first.people().getFirst().interests()).contains("Videojuegos", "Tecnología");
        assertThat(people.count()).isEqualTo(4); assertThat(events.count()).isEqualTo(2);
        assertThat(events.findById(own.getId())).isPresent(); assertThat(metrics(first.eventId()).available()).isZero();
    }
    @Test void concurrentPreparationCreatesOnlyOneScenario() throws Exception {
        try(var pool = Executors.newFixedThreadPool(2)) {
            var start = new CountDownLatch(1);
            var a = pool.submit(() -> { start.await(); return preparation.prepare(); });
            var b = pool.submit(() -> { start.await(); return preparation.prepare(); }); start.countDown();
            assertThat(a.get(15, TimeUnit.SECONDS).eventId()).isEqualTo(b.get(15, TimeUnit.SECONDS).eventId());
        }
        assertThat(events.count()).isEqualTo(1); assertThat(people.count()).isEqualTo(4);
    }
    @Test void metricsTrackReservationsCompletionPointsAndOptionalFeedbackPerEvent() {
        var s = preparation.prepare(); long a = id(s,"Samuel"), b = id(s,"Julián"); available(a,true); available(b,true);
        EventForm f = new EventForm(); f.setName("Otro evento"); long other = eventService.create(f).getId();
        assertThat(metrics(s.eventId()).free()).isEqualTo(2);
        long match = matching.search(a); var offered = metrics(s.eventId());
        assertThat(offered.participants()).isEqualTo(4); assertThat(offered.available()).isEqualTo(2); assertThat(offered.free()).isZero();
        assertThat(offered.offered()).isEqualTo(1); assertThat(offered.active()).isZero(); assertThat(offered.points()).isZero();
        matchService.accept(match,a); matchService.accept(match,b);
        assertThat(metrics(s.eventId()).active()).isEqualTo(1); assertThat(metrics(s.eventId()).offered()).isZero();
        matchService.confirm(match,a); assertThat(metrics(s.eventId()).completed()).isZero();
        matchService.confirm(match,b); matchService.confirm(match,b);
        var completed = metrics(s.eventId()); assertThat(completed.completed()).isEqualTo(1); assertThat(completed.points()).isEqualTo(20); assertThat(completed.free()).isEqualTo(2);
        matchService.giveFeedback(match,a,Feedback.Rating.USEFUL); matchService.giveFeedback(match,a,Feedback.Rating.USEFUL);
        assertThat(metrics(s.eventId()).useful()).isEqualTo(1); assertThat(metrics(s.eventId()).neutral()).isZero();
        long rejected = matching.search(a); matchService.cancel(rejected,b);
        assertThat(metrics(s.eventId()).cancelled()).isEqualTo(1); assertThat(metrics(s.eventId()).points()).isEqualTo(20);
        assertThat(metrics(other).participants()).isZero(); assertThat(metrics(other).points()).isZero();
        preparation.prepare(); assertThat(participants.get(a).points()).isEqualTo(10); assertThat(participants.get(a).available()).isTrue();
    }
    @Test void pausedOrClosedScenarioIsNotSilentlyReopened() {
        var s = preparation.prepare(); available(id(s,"Samuel"),true); available(id(s,"Julián"),true);
        assertThat(metrics(s.eventId()).free()).isEqualTo(2); eventService.transition(s.eventId(), EventStatus.PAUSED);
        assertThat(metrics(s.eventId()).available()).isEqualTo(2); assertThat(metrics(s.eventId()).free()).isZero();
        assertThat(preparation.prepare().status()).isEqualTo(EventStatus.PAUSED);
        eventService.transition(s.eventId(), EventStatus.CLOSED); assertThat(preparation.prepare().status()).isEqualTo(EventStatus.CLOSED);
        assertThat(metrics(s.eventId()).free()).isZero();
    }
    @Test void webUtilityMetricsAndThemeGalleryAreAccessibleWithoutMutatingOnGet() throws Exception {
        try(var browser = new TestBrowser(port)) {
            assertThat(browser.get("/demo/themes").statusCode()).isEqualTo(200);
            assertThat(browser.get("/admin").statusCode()).isEqualTo(200);
            assertThat(browser.get("/demo/prepare").statusCode()).isEqualTo(200); assertThat(events.count()).isZero();
            assertThat(browser.post("/demo/prepare",Map.of()).statusCode()).isEqualTo(302);
            var s = preparation.current(); assertThat(browser.get("/demo/prepare").body()).contains("Samuel", "Julián", "Ana", "Carlos");
            assertThat(browser.post("/demo/prepare",Map.of()).statusCode()).isEqualTo(302); assertThat(events.count()).isEqualTo(1);
            long samuel = id(s,"Samuel"); String page = browser.get("/demo?participantId="+samuel).body();
            assertThat(page).contains("selected=\"selected\"");
            browser.post("/demo",Map.of("participantId",""+samuel)); assertThat(browser.get("/me").statusCode()).isEqualTo(200);
            assertThat(browser.get("/admin").body()).contains("Feria Universitaria 2026", "Libres para conectar", "Puntos entregados");
        }
    }
    @Test void preparationRequiresPostAndFormToken() throws Exception {
        try(var browser = new TestBrowser(port)) {
            assertThat(browser.post("/demo/prepare",Map.of()).statusCode()).isEqualTo(403);
            assertThat(events.count()).isZero();
        }
    }
    @Test void invalidDemoInputIs400WithoutChangingAvailability() throws Exception {
        var s = preparation.prepare(); long a = id(s,"Samuel");
        try(var browser = new TestBrowser(port)) {
            browser.get("/demo");
            assertThat(browser.post("/demo",Map.of("participantId","incorrecto")).statusCode()).isEqualTo(400);
            assertThat(browser.post("/demo",Map.of()).statusCode()).isEqualTo(400);
            browser.post("/demo",Map.of("participantId",""+a)); browser.get("/me");
            assertThat(browser.post("/me/availability",Map.of("available","true")).statusCode()).isEqualTo(400);
            assertThat(browser.post("/me/availability",Map.of("available","incorrecto","version",""+participants.get(a).version())).statusCode()).isEqualTo(400);
            assertThat(participants.get(a).available()).isFalse();
        }
    }
    @Test void liveStateReflectsOtherPersonsActionsAndHistoryIsPersonal() throws Exception {
        var s = preparation.prepare(); long a = id(s,"Samuel"), b = id(s,"Julián"); available(a,true); available(b,true);
        try(var browser = new TestBrowser(port); var anonymous = new TestBrowser(port)) {
            assertThat(anonymous.get("/me/state").statusCode()).isEqualTo(409);
            browser.get("/demo"); browser.post("/demo",Map.of("participantId",""+a));
            var initial = browser.get("/me/state"); assertThat(initial.headers().firstValue("Cache-Control")).contains("no-store");
            long match = matching.search(a); var offered = browser.get("/me/state"); assertThat(offered.body()).isNotEqualTo(initial.body());
            matchService.accept(match,b); var accepted = browser.get("/me/state"); assertThat(accepted.body()).isNotEqualTo(offered.body());
            assertThat(accepted.body()).doesNotContain("Entrada principal", "PIXEL", "Julián");
            matchService.accept(match,a); matchService.confirm(match,a); matchService.confirm(match,b);
            available(a,false); available(b,false);
            long c = id(s,"Ana"), d = id(s,"Carlos"); available(c,true); available(d,true);
            long other = matching.search(c); matchService.accept(other,c); matchService.accept(other,d); matchService.confirm(other,c); matchService.confirm(other,d);
            assertThat(browser.get("/me").body()).contains("<span>1</span> conexiones completadas por ti.");
            assertThat(metrics(s.eventId()).completed()).isEqualTo(2);
        }
    }

}
