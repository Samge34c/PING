package com.ping.admin;

import java.util.*;
import com.ping.auth.*;
import com.ping.encounter.*;
import com.ping.event.*;
import com.ping.match.*;
import com.ping.participant.*;
import com.ping.support.TestBrowser;
import com.ping.verification.CredentialService;
import com.ping.web.BusinessRuleException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;

/**
 * DEC-079 with fictitious data: what each result figure counts, who may see results (server side, not only the menu),
 * the optional door total and the rule that «Comprobar al entregar» is no longer offered to new activities.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {"spring.datasource.url=jdbc:h2:mem:results;MODE=MySQL;DB_CLOSE_DELAY=-1", "spring.datasource.username=sa", "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=validate"})
@ActiveProfiles("verification-test")
class EventResultsTests {
    @Autowired EventService events; @Autowired EncounterService config; @Autowired ParticipantService people; @Autowired MatchingService matching;
    @Autowired MatchService matches; @Autowired CredentialService credentials; @Autowired AccountService accounts; @Autowired AdminService admin;
    @Autowired JdbcTemplate sql; @Autowired PasswordEncoder encoder; @LocalServerPort int port;
    static final String PASSWORD = "Contraseña exclusiva de fixture2026";

    long account() { String email = UUID.randomUUID() + "@fixture.test"; sql.update("insert into ping_accounts(email,password_hash,email_verified) values(?,?,true)", email, encoder.encode(PASSWORD)); return accounts.find(email).orElseThrow(); }
    long general() { String email = "general@fixture.test"; if (accounts.find(email).isEmpty()) accounts.bootstrap(email, PASSWORD); return accounts.find(email).orElseThrow(); }
    long openEvent(String name) {
        var f = new EventForm(); f.setName(name + " " + UUID.randomUUID()); long id = events.create(f).getId(); events.transition(id, EventStatus.OPEN);
        var p = new PointForm(); p.setName("Patio"); p.setDirections("Junto al letrero"); long point = config.savePoint(id, null, p);
        var a = new ActivityForm(); a.setTitle("Canción"); a.setInstructions("Recomiéndense una canción."); a.setRewardPoints(10); a.setPointIds(Set.of(point)); config.saveActivity(id, null, a);
        return id;
    }
    long person(long event, String alias) { var f = new ParticipantForm(); f.setAlias(alias); f.setEventId(event); f.setAvailable(true); long id = people.create(f).id(); accounts.bind(account(), id, event); return id; }
    String key() { return UUID.randomUUID().toString(); }
    long complete(long x, long y) {
        long m = matching.search(x); matches.accept(m, x); matches.accept(m, y);
        matches.arrive(m, x); matches.arrive(m, y);
        var cx = credentials.encounter(m, x, key()); var cy = credentials.encounter(m, y, key());
        credentials.verify(m, x, cy.manual(), "fixture"); credentials.verify(m, y, cx.manual(), "fixture");
        matches.confirm(m, x); matches.confirm(m, y);
        assertThat(matches.get(m, x).status()).isEqualTo(MatchStatus.COMPLETED);
        return m;
    }
    EventMetrics metrics(long event) { return admin.metrics().stream().filter(e -> e.id() == event).findFirst().orElseThrow(); }
    void login(TestBrowser b, long account) throws Exception { b.get("/login"); assertThat(b.post("/login", Map.of("email", accounts.user(account).email(), "password", PASSWORD)).statusCode()).isEqualTo(302); }

    long structured(String name, int points) {
        var f = new EventForm(); f.setName(name + " " + UUID.randomUUID()); long id = events.create(f).getId(); events.transition(id, EventStatus.OPEN);
        var p = new PointForm(); p.setName("Patio"); p.setDirections("Junto al letrero"); long point = config.savePoint(id, null, p);
        var a = new ActivityForm(); a.setTitle("Actividad de " + points); a.setInstructions("Compartan algo."); a.setRewardPoints(points); a.setPointIds(Set.of(point));
        a.setVerificationMethod(CheckMethod.BILATERAL); config.saveActivity(id, null, a);
        return id;
    }
    long credited(long match) { return sql.queryForObject("select count(*) from match_completion_steps where match_id=? and action like 'GRANT%'", Long.class, match); }

    /**
     * DEC-079 «revisión antes de acreditar» is accepted by Samuel but still waits for Julián, so V28 is not applied: every event keeps
     * today's policy. A 0-point activity and the basic mode are different things: the first closes with both confirmations and credits
     * nothing; the basic mode (no structured catalogue) still credits +10. Both count as completed connections in Resultados.
     */
    @Test void zeroPointActivityIsNotTheBasicModeAndBothCountAsConnections() {
        long zero = structured("Cero puntos", 0);
        long a = person(zero, "Ana"), b = person(zero, "Beto");
        long m = complete(a, b);
        assertThat(people.get(a).points()).as("0-point activity: both confirmations close it, no credit").isZero();
        assertThat(people.get(b).points()).isZero();
        assertThat(metrics(zero).completed()).as("still a completed connection in Resultados").isEqualTo(1);
        assertThat(metrics(zero).connectedPeople()).isEqualTo(2);
        assertThat(metrics(zero).points()).isZero();
        assertThat(matches.get(m, a).rewardPoints()).isZero();

        var basicForm = new EventForm(); basicForm.setName("Modo básico " + UUID.randomUUID()); long basic = events.create(basicForm).getId(); events.transition(basic, EventStatus.OPEN);
        assertThat(events.get(basic).isStructuredEncounter()).isFalse();
        long c = person(basic, "Carla"), d = person(basic, "Dario");
        complete(c, d);
        assertThat(people.get(c).points()).as("basic mode keeps LEGACY_REWARD_POINTS until V28 is accepted").isEqualTo(EncounterService.LEGACY_REWARD_POINTS);
        assertThat(people.get(d).points()).isEqualTo(10);
        assertThat(metrics(basic).completed()).isEqualTo(1);

        long fifteen = structured("Con puntos", 15);
        long e = person(fifteen, "Eva"), g = person(fifteen, "Gabo");
        long paid = complete(e, g);
        assertThat(people.get(e).points()).as("measured today: bilateral activity with points credits without staff review (pending V28)").isEqualTo(15);
        assertThat(credited(paid)).as("one GRANT per person, once").isEqualTo(2);
    }

    @Test void uniquePeopleAreCountedOnceByIdAndOnlyFromCompletedEncounters() {
        long event = openEvent("Resultados ficticios");
        long ana = person(event, "Ana"), samuel = person(event, "Samuel");
        complete(ana, samuel);
        long julian = person(event, "Julián");
        complete(ana, julian);
        // A second «Ana» is a different person: aliases are not identities.
        long otherAna = person(event, "Ana"), carlos = person(event, "Carlos");
        long cancelled = matching.search(otherAna); matches.cancel(cancelled, otherAna);
        long offered = matching.search(carlos);
        var m = metrics(event);
        assertThat(m.completed()).as("connections are not multiplied by two").isEqualTo(2);
        assertThat(m.connectedPeople()).as("Ana + Samuel, then Ana + Julián: three people").isEqualTo(3);
        assertThat(m.cancelled()).isEqualTo(1);
        assertThat(m.offered() + m.active()).as("the open proposal counts as a proposal, not as a connection").isEqualTo(1);
        assertThat(m.proposals()).isEqualTo(4);
        assertThat(m.completionPercent()).isEqualTo(50);
        assertThat(m.participants()).isEqualTo(5);
        assertThat(offered).isPositive();
        // Another event never leaks into these figures.
        long other = openEvent("Control");
        complete(person(other, "Dana"), person(other, "Eva"));
        assertThat(metrics(event).connectedPeople()).isEqualTo(3);
        assertThat(metrics(other).connectedPeople()).isEqualTo(2);
    }

    @Test void emptyEventAndDoorTotalMissingZeroValidOrInconsistent() {
        long event = openEvent("Puerta ficticia");
        var empty = metrics(event);
        assertThat(empty.participants()).isZero(); assertThat(empty.connectedPeople()).isZero(); assertThat(empty.proposals()).isZero();
        assertThat(empty.completionPercent()).isZero(); assertThat(empty.claims().total()).isZero();
        assertThat(empty.doorState()).isEqualTo(EventMetrics.Door.MISSING); assertThat(empty.doorPercent()).isNull();
        complete(person(event, "Ana"), person(event, "Samuel"));
        admin.recordDoor(event, 0);
        assertThat(metrics(event).doorState()).isEqualTo(EventMetrics.Door.ZERO); assertThat(metrics(event).doorPercent()).isNull();
        admin.recordDoor(event, 1);
        assertThat(metrics(event).doorState()).as("two connected people cannot come from one entry").isEqualTo(EventMetrics.Door.REVIEW);
        assertThat(metrics(event).doorPercent()).as("never capped to 100 %").isNull();
        admin.recordDoor(event, 8);
        assertThat(metrics(event).doorState()).isEqualTo(EventMetrics.Door.VALID); assertThat(metrics(event).doorPercent()).isEqualTo(25);
        admin.recordDoor(event, null);
        assertThat(metrics(event).doorState()).isEqualTo(EventMetrics.Door.MISSING);
        assertThatThrownBy(() -> admin.recordDoor(event, -1)).isInstanceOf(BusinessRuleException.class);
    }

    @Test void resultsAndDoorTotalAreForTheEventOrganisationAndGeneralAdministrationOnly() throws Exception {
        long mine = openEvent("Propio"), foreign = openEvent("Ajeno");
        long organizer = account(), staff = account();
        accounts.assign(general(), organizer, mine, true, AccountService.EventRole.ORGANIZER);
        accounts.assign(general(), staff, mine, true, AccountService.EventRole.STAFF);
        try (var b = new TestBrowser(port)) {
            login(b, staff);
            assertThat(b.get("/admin?eventId=" + mine).statusCode()).as("STAFF is denied by the server, not only by the menu").isEqualTo(403);
            assertThat(b.get("/admin").statusCode()).isEqualTo(403);
            assertThat(b.post("/events/" + mine + "/attendance", Map.of("doorAttendance", "50")).statusCode()).isEqualTo(403);
            String board = b.get("/organizer").body();
            assertThat(board).contains("/events/" + mine + "/reviews", "/organizer/redemptions").doesNotContain("/admin?eventId=", "tab=configuracion", "tab=participantes");
            assertThat(b.get("/events/" + mine + "/reviews").statusCode()).as("STAFF keeps its two tasks").isEqualTo(200);
            assertThat(b.get("/organizer/redemptions").statusCode()).isEqualTo(200);
        }
        try (var b = new TestBrowser(port)) {
            login(b, organizer);
            assertThat(b.get("/admin?eventId=" + mine).statusCode()).isEqualTo(200);
            assertThat(b.get("/admin?eventId=" + foreign).statusCode()).isEqualTo(403);
            assertThat(b.get("/admin").body()).contains("data-event-id=\"" + mine + "\"").doesNotContain("data-event-id=\"" + foreign + "\"");
            assertThat(b.post("/events/" + mine + "/attendance", Map.of("doorAttendance", "120")).statusCode()).isEqualTo(302);
            assertThat(metrics(mine).door()).isEqualTo(120);
            for (String invalid : List.of("12.5", "...", "1.20", "12a"))
                { b.post("/events/" + mine + "/attendance", Map.of("doorAttendance", invalid)); assertThat(metrics(mine).door()).as(invalid + " is rejected, not reinterpreted or cleared").isEqualTo(120); }
            b.post("/events/" + mine + "/attendance", Map.of("doorAttendance", "1.200")); assertThat(metrics(mine).door()).as("grouped thousands").isEqualTo(1200);
            b.post("/events/" + mine + "/attendance", Map.of("doorAttendance", "120")); assertThat(metrics(mine).door()).isEqualTo(120);
            assertThat(b.post("/events/" + foreign + "/attendance", Map.of("doorAttendance", "5")).statusCode()).isEqualTo(403);
            assertThat(metrics(foreign).door()).isNull();
            assertThat(b.get("/organizer/events").statusCode()).as("general administration only").isEqualTo(403);
        }
        try (var b = new TestBrowser(port)) {
            login(b, general());
            assertThat(b.get("/admin?eventId=" + foreign).statusCode()).isEqualTo(200);
            assertThat(b.get("/organizer/events").body()).contains("/organizer/access?event=" + mine, "/organizer/access?event=" + foreign, "/interests/taxonomy");
        }
    }

    @Test void atDeliveryIsKeptOnlyByActivitiesThatAlreadyHadIt() {
        long event = openEvent("Validación previa");
        long point = config.points(event).getFirst().getId();
        var fresh = new ActivityForm(); fresh.setTitle("Mascota"); fresh.setInstructions("Búsquenla."); fresh.setRewardPoints(20); fresh.setPointIds(Set.of(point));
        fresh.setVerificationMethod(CheckMethod.AT_DELIVERY); fresh.setCheckInstructions("Foto de los dos");
        assertThatThrownBy(() -> config.saveActivity(event, null, fresh)).isInstanceOf(BusinessRuleException.class);
        fresh.setVerificationMethod(CheckMethod.STAFF_REVIEW);
        long reviewed = config.saveActivity(event, null, fresh);
        var switched = config.activityForm(event, reviewed); switched.setVerificationMethod(CheckMethod.AT_DELIVERY);
        assertThatThrownBy(() -> config.saveActivity(event, reviewed, switched)).as("cannot be switched on for another activity").isInstanceOf(BusinessRuleException.class);
        // An activity stored before the change keeps its method when edited (nothing is reinterpreted retroactively).
        sql.update("update event_activities set verification_method='AT_DELIVERY' where id=?", reviewed);
        var kept = config.activityForm(event, reviewed); kept.setTitle("Mascota de la U");
        config.saveActivity(event, reviewed, kept);
        assertThat(config.activityForm(event, reviewed).getVerificationMethod()).isEqualTo(CheckMethod.AT_DELIVERY);
    }
}
