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
        assertThat(first.name()).isEqualTo("SOFA 2026");
        assertThat(eventService.get(first.eventId()).getDescription()).contains("demo académico", "Corferias Bogotá", "8 al 12 de octubre de 2026");
        assertThat(first.people()).extracting(DemoPreparationService.Person::alias).containsExactly("Samuel", "Julián", "Ana", "Carlos");
        assertThat(first.people().getFirst().interests()).containsExactlyInAnyOrder("Videojuegos", "Anime");
        assertThat(first.people().get(1).interests()).containsExactlyInAnyOrder("Videojuegos", "Tecnología");
        assertThat(first.people().get(2).interests()).containsExactlyInAnyOrder("Anime", "Fotografía");
        assertThat(first.people().get(3).interests()).containsExactlyInAnyOrder("Cine", "Música");
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
        matchService.arrive(match,a); matchService.arrive(match,b);
        matchService.confirm(match,a); assertThat(metrics(s.eventId()).completed()).isZero();
        matchService.confirm(match,b); matchService.confirm(match,b);
        var completed = metrics(s.eventId()); assertThat(completed.completed()).isEqualTo(1); assertThat(completed.points()).isEqualTo(20); assertThat(completed.free()).isEqualTo(2);
        matchService.giveFeedback(match,a,Feedback.Rating.USEFUL); matchService.giveFeedback(match,a,Feedback.Rating.USEFUL);
        assertThat(metrics(s.eventId()).useful()).isEqualTo(1); assertThat(metrics(s.eventId()).neutral()).isZero();
        long rejected = matching.search(a); matchService.cancel(rejected,b);
        assertThat(metrics(s.eventId()).cancelled()).isEqualTo(1); assertThat(metrics(s.eventId()).points()).isEqualTo(20);
        var detail = admin.details(s.eventId());
        assertThat(detail.meetingPoint()).isEqualTo("Meet & Greet · Pabellón 23");
        assertThat(detail.participants()).filteredOn(p -> p.id() == a).singleElement().satisfies(p -> {
            assertThat(p.points()).isEqualTo(10); assertThat(p.completed()).isEqualTo(1); assertThat(p.interests()).contains("Videojuegos");
        });
        assertThat(detail.matches()).hasSize(2);
        assertThat(detail.feedback()).singleElement().satisfies(fb -> {
            assertThat(fb.alias()).isEqualTo("Samuel"); assertThat(fb.rating()).isEqualTo("Positivo"); assertThat(fb.createdAt()).isNotNull();
        });
        assertThat(admin.details(other).matches()).isEmpty();
        assertThat(metrics(s.eventId()).completionPercent()).isEqualTo(50);
        assertThat(metrics(s.eventId()).unavailable()).isEqualTo(2);
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
            assertThat(browser.get("/admin").body()).contains("SOFA 2026", "Libres para conectar", "Puntos entregados");
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
            assertThat(anonymous.get("/me/state").statusCode()).isEqualTo(401);
            browser.get("/demo"); browser.post("/demo",Map.of("participantId",""+a));
            var initial = browser.get("/me/state"); assertThat(initial.headers().firstValue("Cache-Control")).contains("no-store");
            long match = matching.search(a); var offered = browser.get("/me/state"); assertThat(offered.body()).isNotEqualTo(initial.body());
            matchService.accept(match,b); var accepted = browser.get("/me/state"); assertThat(accepted.body()).isNotEqualTo(offered.body());
            assertThat(accepted.body()).doesNotContain("Entrada principal", "PIXEL", "Julián");
            matchService.accept(match,a); matchService.arrive(match,a); matchService.arrive(match,b); matchService.confirm(match,a); matchService.confirm(match,b);
            available(a,false); available(b,false);
            long c = id(s,"Ana"), d = id(s,"Carlos"); available(c,true); available(d,true);
            long other = matching.search(c); matchService.accept(other,c); matchService.accept(other,d); matchService.arrive(other,c); matchService.arrive(other,d); matchService.confirm(other,c); matchService.confirm(other,d);
            assertThat(browser.get("/me").body()).contains("<span>1</span> conexiones completadas por ti.");
            assertThat(metrics(s.eventId()).completed()).isEqualTo(2);
        }
    }

    @Test void adminFilterShowsOnlySelectedEventAndKeepsItOnRefresh() throws Exception {
        var scenario = preparation.prepare(); EventForm form = new EventForm(); form.setName("Otro evento de prueba");
        long other = eventService.create(form).getId();
        try(var browser = new TestBrowser(port)) {
            var all = browser.get("/admin"); assertThat(all.body()).contains("data-event-id=\""+scenario.eventId()+"\"", "data-event-id=\""+other+"\"");
            var filtered = browser.get("/admin?eventId="+scenario.eventId());
            assertThat(filtered.statusCode()).isEqualTo(200);
            assertThat(filtered.body()).contains("data-event-id=\""+scenario.eventId()+"\"", "/admin?eventId="+scenario.eventId());
            assertThat(filtered.body()).doesNotContain("data-event-id=\""+other+"\"");
            assertThat(browser.get("/admin?eventId=").body()).contains("data-event-id=\""+other+"\"");
        }
    }
    @Test void adminFilterHandlesUnknownAndMalformedEventIds() throws Exception {
        try(var browser = new TestBrowser(port)) {
            assertThat(browser.get("/admin?eventId=999999").statusCode()).isEqualTo(404);
            assertThat(browser.get("/admin?eventId=incorrecto").statusCode()).isEqualTo(400);
        }
    }

    @Test void viewingAnotherParticipantNeverAssignsTheirAliasToSelectedSessionInNavbar() throws Exception {
        var scenario = preparation.prepare();
        long samuel = id(scenario, "Samuel"), julian = id(scenario, "Julián");
        try (var browser = new TestBrowser(port)) {
            browser.get("/demo");
            browser.post("/demo", Map.of("participantId", Long.toString(samuel)));
            assertThat(browser.get("/me").body()).contains("data-session-name=\"Samuel\"");
            for (String route : List.of("/participants/" + julian, "/participants/" + julian + "/interests")) {
                var response = browser.get(route);
                assertThat(response.statusCode()).isEqualTo(200);
                var chip = java.util.regex.Pattern.compile("<div class=\"session-chip\"([^>]*)>").matcher(response.body());
                assertThat(chip.find()).isTrue();
                assertThat(chip.group(1)).contains("data-session-id=\"" + samuel + "\"")
                        .doesNotContain("data-session-name=\"Julián\"");
            }
            assertThat(browser.get("/me/state").body()).contains(samuel + ":");
            assertThat(browser.get("/me").body()).contains("data-session-name=\"Samuel\"");
        }
    }

    @Test void alternativeOfferHeadingDoesNotClaimSharedAffinity() throws Exception {
        var scenario = preparation.prepare();
        long ana = id(scenario, "Ana"), carlos = id(scenario, "Carlos");
        available(ana, true); available(carlos, true);
        long match = matching.search(ana);
        try (var browser = new TestBrowser(port)) {
            browser.get("/demo");
            browser.post("/demo", Map.of("participantId", Long.toString(ana)));
            var response = browser.get("/matches/" + match);
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("DESCUBRE OTROS INTERESES", "Alternativa · 0 intereses compartidos")
                    .doesNotContain("ENCONTRAMOS AFINIDAD");
            assertThat(participants.get(ana).points()).isZero();
            assertThat(participants.get(carlos).points()).isZero();
        }
    }


    @Test void legacyScenarioIsAdaptedOnceWithoutResettingProgressOrExistingEncounter() {
        var scenario = preparation.prepare(); long a = id(scenario, "Samuel"), b = id(scenario, "Julián");
        EventForm legacy = EventForm.from(eventService.get(scenario.eventId()));
        legacy.setName("Feria Universitaria 2026"); legacy.setMeetingPoint("Entrada principal");
        eventService.update(scenario.eventId(), legacy);
        available(a, true); available(b, true); long match = matching.search(a);
        matchService.accept(match, a); matchService.accept(match, b);
        var before = matchService.get(match, a);
        matchService.arrive(match, a); matchService.arrive(match, b);
        matchService.confirm(match, a); matchService.confirm(match, b);
        long active = matching.search(a); matchService.accept(active, a); matchService.accept(active, b);
        var activeBefore = matchService.get(active, a);
        eventService.transition(scenario.eventId(), EventStatus.PAUSED);
        sql.update("update participants set points = 23 where id = ?", a);
        var adapted = preparation.prepare();
        assertThat(adapted.eventId()).isEqualTo(scenario.eventId());
        assertThat(adapted.name()).isEqualTo("SOFA 2026"); assertThat(adapted.status()).isEqualTo(EventStatus.PAUSED);
        assertThat(adapted.people()).extracting(DemoPreparationService.Person::id).containsExactlyElementsOf(
            scenario.people().stream().map(DemoPreparationService.Person::id).toList());
        assertThat(participants.get(a).points()).isEqualTo(23); assertThat(participants.get(b).points()).isEqualTo(10);
        assertThat(participants.get(a).available()).isTrue(); assertThat(matches.count()).isEqualTo(2);
        assertThat(matchService.get(match, a).activity()).isEqualTo(before.activity());
        assertThat(matchService.get(active, a)).isEqualTo(activeBefore);
        assertThat(adapted.people().getFirst().interests()).containsExactlyInAnyOrder("Videojuegos", "Anime");
        // Repeat prepare must preserve subsequent user edits as well as progress.
        sql.update("delete from participant_interests where participant_id = ?", a);
        assertThat(preparation.prepare().people().getFirst().interests()).isEmpty();
        assertThat(events.count()).isEqualTo(1); assertThat(people.count()).isEqualTo(4);
    }
}
