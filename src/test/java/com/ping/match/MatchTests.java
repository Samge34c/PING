package com.ping.match;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import com.ping.event.*;
import com.ping.participant.*;
import com.ping.interest.*;
import com.ping.support.TestBrowser;
import com.ping.web.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:match-tests;MODE=MySQL;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class MatchTests {
    @Autowired MatchingService matching;
    @Autowired MatchService service;
    @Autowired MatchRepository matches;
    @Autowired MatchReservationRepository reservations;
    @Autowired FeedbackRepository feedback;
    @Autowired ParticipantService participants;
    @Autowired ParticipantRepository participantRepository;
    @Autowired InterestService interests;
    @Autowired EventService events;
    @Autowired EventRepository eventRepository;
    @Autowired JdbcTemplate sql;
    @LocalServerPort int port;
    Event event;
    @BeforeEach void setup() {
        feedback.deleteAll(); reservations.deleteAll(); matches.deleteAll(); participantRepository.deleteAll(); eventRepository.deleteAll();
        event = openEvent("Feria Universitaria 2026");
    }
    Event openEvent(String name) {
        EventForm form = new EventForm(); form.setName(name); form.setMeetingPoint("Entrada principal");
        Event e = events.create(form); return events.transition(e.getId(), EventStatus.OPEN);
    }
    long participant(Event event, String alias, String... selected) {
        ParticipantForm f = new ParticipantForm(); f.setAlias(alias); f.setEventId(event.getId()); f.setAvailable(true);
        var p = participants.create(f);
        InterestSelectionForm selection = new InterestSelectionForm(); selection.setVersion(p.version());
        selection.setInterestIds(interests.catalog().stream().filter(i -> Arrays.asList(selected).contains(i.getName())).map(Interest::getId).toList());
        interests.save(p.id(), selection); return p.id();
    }
    long p(String name, String... selected) { return participant(event, name, selected); }
    long[] pair() { return new long[]{p("Samuel", "Videojuegos", "Tecnología"), p("Julián", "Videojuegos", "Música")}; }
    void available(long id, boolean value) {
        AvailabilityForm form = new AvailabilityForm(); form.setAvailable(value); form.setVersion(participants.get(id).version()); participants.setAvailability(id, form);
    }
    long activate(long[] pair) { long id = matching.search(pair[0]); service.accept(id, pair[0]); service.accept(id, pair[1]); return id; }
    @Test void commonInterestAndBestAffinityWinOverWaiting() {
        long self = p("Samuel", "Videojuegos", "Tecnología");
        long older = p("Mayor espera", "Videojuegos");
        long best = p("Mejor afinidad", "Videojuegos", "Tecnología");
        sql.update("update participants set waiting_since = ? where id = ?", java.sql.Timestamp.from(Instant.parse("2020-01-01T00:00:00Z")), older);
        long id = matching.search(self);
        assertThat(service.get(id, self).otherAlias()).isEqualTo("Mejor afinidad");
        assertThat(service.get(id, best).commonInterests()).isEqualTo("Tecnología · Videojuegos");
        assertThat(reservations.count()).isEqualTo(2);
    }
    @Test void tieUsesWaitingBeforeId() {
        long self = p("Samuel", "Cine"); p("ID menor", "Cine"); long oldest = p("Espera mayor", "Cine");
        sql.update("update participants set waiting_since = ? where id = ?", java.sql.Timestamp.from(Instant.parse("2020-01-01T00:00:00Z")), oldest);
        assertThat(service.get(matching.search(self), self).otherAlias()).isEqualTo("Espera mayor");
    }
    @Test void equalWaitingUsesAscendingId() {
        long self = p("Samuel", "Cine"); long first = p("Primero", "Cine"); p("Segundo", "Cine");
        sql.update("update participants set waiting_since = ?", java.sql.Timestamp.from(Instant.parse("2020-01-01T00:00:00Z")));
        long id = matching.search(self); assertThat(service.get(id, first).otherAlias()).isEqualTo("Samuel");
    }
    @Test void zeroAffinityOffersOldestAlternativeAndRequiresBoth() {
        long self = p("Samuel", "Videojuegos"); long alternative = p("Julián", "Cine"); p("Después", "Música");
        long id = matching.search(self); var view = service.get(id, self);
        assertThat(view.commonInterests()).isEmpty(); assertThat(view.otherInterests()).isEqualTo("Cine");
        service.accept(id, self); assertThat(service.get(id, self).status()).isEqualTo(MatchStatus.OFFERED);
        service.accept(id, alternative); assertThat(service.get(id, self).word()).startsWith("PING-");
        assertThat(service.get(id, self).activity()).contains("de acuerdo");
    }
    @Test void excludesUnavailableAndOtherEvents() {
        long self = p("Samuel", "Videojuegos"); long unavailable = p("No disponible", "Videojuegos"); available(unavailable, false);
        participant(openEvent("Otra feria"), "Otro evento", "Videojuegos");
        assertThatThrownBy(() -> matching.search(self)).isInstanceOf(BusinessRuleException.class); assertThat(matches.count()).isZero();
    }
    @Test void unavailableSearcherCannotCreateOffer() {
        long[] pair = pair(); available(pair[0], false);
        assertThatThrownBy(() -> matching.search(pair[0])).isInstanceOf(BusinessRuleException.class);
    }
    @ParameterizedTest @EnumSource(value = EventStatus.class, names = {"PAUSED", "CLOSED"})
    void pausedAndClosedCannotCreateOrActivate(EventStatus status) {
        long[] pair = pair(); long id = matching.search(pair[0]); events.transition(event.getId(), status);
        assertThatThrownBy(() -> service.accept(id, pair[0])).isInstanceOf(BusinessRuleException.class);
        service.cancel(id, pair[0]);
        assertThatThrownBy(() -> matching.search(pair[0])).isInstanceOf(BusinessRuleException.class);
        assertThat(matches.count()).isEqualTo(1);
    }
    @Test void oneAcceptanceDoesNotRevealMeetingButTwoActivate() {
        long[] pair = pair(); long id = matching.search(pair[0]);
        assertThat(service.get(id, pair[0]).meetingPoint()).isNull();
        service.accept(id, pair[0]); service.accept(id, pair[0]);
        var waiting = service.get(id, pair[0]); assertThat(waiting.status()).isEqualTo(MatchStatus.OFFERED);
        assertThat(waiting.word()).isNull(); assertThat(waiting.activity()).isNull();
        service.accept(id, pair[1]); var active = service.get(id, pair[0]);
        assertThat(active.status()).isEqualTo(MatchStatus.ACTIVE); assertThat(active.meetingPoint()).isEqualTo("Entrada principal");
        assertThat(active.word()).startsWith("PIXEL-"); assertThat(active.activity()).contains("videojuego");
        service.accept(id, pair[0]); assertThat(service.get(id, pair[1]).word()).isEqualTo(active.word());
    }
    @Test void snapshotsKeepWordActivityAndMeetingAfterChanges() {
        long[] pair = pair(); long id = activate(pair); var before = service.get(id, pair[0]);
        EventForm form = EventForm.from(events.get(event.getId())); form.setMeetingPoint("Cafetería"); events.update(event.getId(), form);
        InterestSelectionForm selected = interests.selection(pair[0]); selected.setInterestIds(List.of()); interests.save(pair[0], selected);
        var after = service.get(id, pair[0]); assertThat(after.word()).isEqualTo(before.word());
        assertThat(after.meetingPoint()).isEqualTo("Entrada principal"); assertThat(after.commonInterests()).isEqualTo("Videojuegos");
    }
    @Test void bothConfirmAndRetriesRewardOnlyOnceAndAllowAnotherSearch() {
        long[] pair = pair(); long id = activate(pair);
        service.confirm(id, pair[0]); service.confirm(id, pair[0]);
        assertThat(service.get(id, pair[0]).status()).isEqualTo(MatchStatus.ACTIVE); assertThat(participants.get(pair[0]).points()).isZero();
        service.confirm(id, pair[1]); service.confirm(id, pair[1]); service.confirm(id, pair[0]);
        assertThat(service.get(id, pair[0]).status()).isEqualTo(MatchStatus.COMPLETED);
        assertThat(participants.get(pair[0]).points()).isEqualTo(10); assertThat(participants.get(pair[1]).points()).isEqualTo(10);
        assertThat(reservations.count()).isZero(); long next = activate(pair);
        assertThat(next).isNotEqualTo(id); service.confirm(next, pair[0]); service.confirm(next, pair[1]);
        assertThat(participants.get(pair[0]).points()).isEqualTo(20);
    }
    @Test void rejectReleasesBothAndDoesNotImmediatelyRepeatPair() {
        long[] pair = pair(); long id = matching.search(pair[0]); service.accept(id, pair[0]); service.cancel(id, pair[1]); service.cancel(id, pair[1]);
        assertThat(service.get(id, pair[0]).status()).isEqualTo(MatchStatus.CANCELLED); assertThat(reservations.count()).isZero();
        assertThat(participants.get(pair[0]).available()).isTrue(); assertThat(participants.get(pair[1]).available()).isTrue();
        assertThatThrownBy(() -> matching.search(pair[0])).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> matching.search(pair[1])).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.accept(id, pair[0])).isInstanceOf(BusinessRuleException.class);
        available(pair[0], false); available(pair[0], true); assertThat(matching.search(pair[0])).isNotEqualTo(id);
    }
    @Test void activeCancellationNoPointsAndCompletedCannotCancel() {
        long[] pair = pair(); long id = activate(pair); service.cancel(id, pair[0]);
        assertThat(participants.get(pair[0]).points()).isZero(); assertThat(reservations.count()).isZero();
        available(pair[0], false); available(pair[0], true); long next = activate(pair);
        service.confirm(next, pair[0]); service.confirm(next, pair[1]);
        assertThatThrownBy(() -> service.cancel(next, pair[0])).isInstanceOf(BusinessRuleException.class);
    }
    @Test void offeredAndActiveParticipantsCannotHaveSecondOfferOrChangeAvailability() {
        long[] pair = pair(); long third = p("Tercero", "Videojuegos"); long id = matching.search(pair[0]);
        assertThat(matching.search(pair[1])).isEqualTo(id); assertThat(matching.search(pair[0])).isEqualTo(id);
        assertThatThrownBy(() -> matching.search(third)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> available(pair[0], false)).isInstanceOf(BusinessRuleException.class);
        service.accept(id, pair[0]); service.accept(id, pair[1]);
        assertThatThrownBy(() -> matching.search(third)).isInstanceOf(BusinessRuleException.class); assertThat(matches.count()).isEqualTo(1);
    }
    @Test void concurrentSearchesDoNotDoubleReserve() throws Exception {
        long a = p("A", "Cine"), b = p("B", "Cine"), c = p("C", "Cine");
        try (var pool = Executors.newFixedThreadPool(2)) {
            var start = new CountDownLatch(1);
            var first = pool.submit(() -> { start.await(); return attemptSearch(a); });
            var second = pool.submit(() -> { start.await(); return attemptSearch(c); }); start.countDown();
            long x = first.get(15, TimeUnit.SECONDS), y = second.get(15, TimeUnit.SECONDS);
            assertThat(x > 0 || y > 0).isTrue(); assertThat(matches.count()).isEqualTo(1); assertThat(reservations.count()).isEqualTo(2);
        }
    }
    long attemptSearch(long id) { try { return matching.search(id); } catch (BusinessRuleException error) { return -1; } }
    @Test void concurrentConfirmationsCreditExactlyOnce() throws Exception {
        long[] pair = pair(); long id = activate(pair);
        try (var pool = Executors.newFixedThreadPool(4)) {
            var start = new CountDownLatch(1); List<Future<?>> requests = new ArrayList<>();
            for (int i = 0; i < 4; i++) { long person = pair[i % 2]; requests.add(pool.submit(() -> { start.await(); service.confirm(id, person); return null; })); }
            start.countDown(); for (var result : requests) result.get(15, TimeUnit.SECONDS);
        }
        assertThat(service.get(id, pair[0]).status()).isEqualTo(MatchStatus.COMPLETED);
        assertThat(participants.get(pair[0]).points()).isEqualTo(10); assertThat(participants.get(pair[1]).points()).isEqualTo(10);
    }
    @Test void activeCanFinishAfterEventCloses() {
        long[] pair = pair(); long id = activate(pair); events.transition(event.getId(), EventStatus.CLOSED);
        service.confirm(id, pair[0]); service.confirm(id, pair[1]); assertThat(participants.get(pair[0]).points()).isEqualTo(10);
    }
    @Test void feedbackOptionalPersistedAndRetriesNeverChangePoints() {
        long[] pair = pair(); long id = activate(pair);
        assertThatThrownBy(() -> service.giveFeedback(id, pair[0], Feedback.Rating.USEFUL)).isInstanceOf(BusinessRuleException.class);
        service.confirm(id, pair[0]); service.confirm(id, pair[1]);
        service.giveFeedback(id, pair[0], Feedback.Rating.USEFUL); service.giveFeedback(id, pair[0], Feedback.Rating.USEFUL);
        assertThat(feedback.count()).isEqualTo(1); assertThat(service.get(id, pair[0]).feedback()).isEqualTo("USEFUL");
        assertThat(service.get(id, pair[1]).feedback()).isNull(); assertThat(participants.get(pair[0]).points()).isEqualTo(10);
    }
    @Test void outsidersCannotReadAcceptConfirmCancelOrReviewOthersMatch() {
        long[] pair = pair(); long outsider = p("Otro", "Cine"); long id = activate(pair);
        assertThatThrownBy(() -> service.get(id, outsider)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.accept(id, outsider)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.confirm(id, outsider)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.cancel(id, outsider)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.giveFeedback(id, outsider, Feedback.Rating.NEUTRAL)).isInstanceOf(NotFoundException.class);
    }
    @Test void twoBrowserSessionsCompleteFullFlowWithSessionOwnedActions() throws Exception {
        long[] pair = pair(); long outsider = p("Otro", "Cine");
        try (var samuel = new TestBrowser(port); var julian = new TestBrowser(port); var other = new TestBrowser(port)) {
            assertThat(samuel.get("/me").statusCode()).isEqualTo(302);
            samuel.get("/demo"); julian.get("/demo"); other.get("/demo");
            assertThat(samuel.post("/demo", Map.of("participantId", "" + pair[0])).statusCode()).isEqualTo(302);
            julian.post("/demo", Map.of("participantId", "" + pair[1])); other.post("/demo", Map.of("participantId", "" + outsider));
            assertThat(samuel.get("/me").body()).contains("Samuel", "Buscar conexión", "Modo demo");
            var offer = samuel.post("/me/search", Map.of()); String path = java.net.URI.create(offer.headers().firstValue("Location").orElseThrow()).getPath();
            assertThat(samuel.get(path).body()).contains("OFFERED", "Julián").doesNotContain("Entrada principal", "PIXEL-");
            assertThat(julian.get("/me").body()).contains(path); assertThat(other.get(path).statusCode()).isEqualTo(404);
            assertThat(other.post(path + "/accept", Map.of("participantId", "" + pair[0])).statusCode()).isEqualTo(404);
            samuel.post(path + "/accept", Map.of("participantId", "" + pair[1]));
            assertThat(julian.get(path).body()).contains("OFFERED", "La otra persona ya aceptó");
            julian.post(path + "/accept", Map.of());
            assertThat(samuel.get(path).body()).contains("ACTIVE", "Entrada principal", "PIXEL-", "videojuego");
            samuel.post(path + "/confirm", Map.of()); assertThat(julian.get(path).body()).contains("ACTIVE");
            julian.post(path + "/confirm", Map.of()); samuel.post(path + "/confirm", Map.of());
            assertThat(samuel.get(path).body()).contains("COMPLETED", "+10", "Omitir");
            assertThat(samuel.post(path + "/feedback", Map.of("rating", "INVALID")).statusCode()).isEqualTo(409);
            samuel.post(path + "/feedback", Map.of("rating", "USEFUL"));
            assertThat(samuel.get(path).body()).contains("feedback quedó guardado");
            assertThat(participants.get(pair[0]).points()).isEqualTo(10); assertThat(participants.get(pair[1]).points()).isEqualTo(10);
        }
    }
}
