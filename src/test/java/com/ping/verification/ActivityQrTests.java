package com.ping.verification;

import java.util.*;
import java.util.concurrent.*;
import com.ping.auth.*;
import com.ping.encounter.*;
import com.ping.event.*;
import com.ping.match.*;
import com.ping.participant.*;
import com.ping.support.TestBrowser;
import com.ping.web.BusinessRuleException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;

/** DEC-081 with fictitious data: activity QR shown after «Completar actividad», one staff scan, Saga credits once. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {"spring.datasource.url=jdbc:h2:mem:activity-qr;MODE=MySQL;DB_CLOSE_DELAY=-1", "spring.datasource.username=sa", "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=validate"})
@ActiveProfiles("verification-test")
class ActivityQrTests {
    @Autowired EventService events; @Autowired EncounterService config; @Autowired ParticipantService people; @Autowired MatchingService matching;
    @Autowired MatchService matches; @Autowired CredentialService credentials; @Autowired AccountService accounts; @Autowired JdbcTemplate sql;
    @Autowired PasswordEncoder encoder; @LocalServerPort int port;
    static final String PASSWORD = "Contraseña exclusiva de fixture2026";
    long event, activity, a, b, match, accountA, accountB, staff;

    @BeforeEach void setup() {
        event = event("QR ficticio", 20, CheckMethod.QR_VALIDATION);
        activity = sql.queryForObject("select id from event_activities where event_id=?", Long.class, event);
        accountA = account(); accountB = account(); a = person(accountA, event, "Lina"); b = person(accountB, event, "Mario");
        match = matching.search(a); matches.accept(match, a); matches.accept(match, b);
        staff = role(event, AccountService.EventRole.STAFF);
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    long event(String name, int points, CheckMethod method) {
        var f = new EventForm(); f.setName(name + " " + UUID.randomUUID()); long id = events.create(f).getId(); events.transition(id, EventStatus.OPEN);
        var p = new PointForm(); p.setName("Mural"); p.setDirections("Junto a la entrada"); long point = config.savePoint(id, null, p);
        var act = new ActivityForm(); act.setTitle("Foto en el mural"); act.setInstructions("Tómense una foto en el mural."); act.setRewardPoints(points); act.setPointIds(Set.of(point));
        act.setVerificationMethod(method); config.saveActivity(id, null, act);
        return id;
    }
    long account() { String email = UUID.randomUUID() + "@fixture.test"; sql.update("insert into ping_accounts(email,password_hash,email_verified) values(?,?,true)", email, encoder.encode(PASSWORD)); return accounts.find(email).orElseThrow(); }
    long person(long account, long event, String name) { var f = new ParticipantForm(); f.setAlias(name); f.setEventId(event); f.setAvailable(true); long id = people.create(f).id(); accounts.bind(account, id, event); return id; }
    long general() { String email = "general@fixture.test"; if (accounts.find(email).isEmpty()) accounts.bootstrap(email, PASSWORD); return accounts.find(email).orElseThrow(); }
    long role(long forEvent, AccountService.EventRole role) { long id = account(); accounts.assign(general(), id, forEvent, true, role); return id; }
    void as(long id) { var user = accounts.user(id); SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities())); }
    String key() { return UUID.randomUUID().toString(); }
    void verified(long m, long x, long y) {
        matches.arrive(m, x); matches.arrive(m, y);
        var cx = credentials.encounter(m, x, key()); var cy = credentials.encounter(m, y, key());
        credentials.verify(m, x, cy.manual(), "fixture"); credentials.verify(m, y, cx.manual(), "fixture");
    }
    String qr(CredentialService.Issued c) { return "PING:" + c.purpose() + ":" + c.token(); }
    /** Staff scan as the controller does it: validate, then (unless waiting) resume the existing Saga outside that transaction. */
    CredentialService.ActivityScan scan(long account, String code) {
        as(account); var result = credentials.validateActivity(code, "fixture"); SecurityContextHolder.clearContext();
        if (result.result() != CredentialService.ScanResult.WAITING) matches.completeAfterReview(result.match());
        return result;
    }
    long grants(long m) { return sql.queryForObject("select count(*) from match_completion_steps where match_id=? and action like 'GRANT%'", Long.class, m); }

    @Test void scanBeforeSecondCompletionWaitsThenOneScanCreditsOnceAndRepeatsChangeNothing() {
        verified(match, a, b);
        assertThat(matches.get(match, a).qrValidation()).as("method frozen at activation").isTrue();
        assertThatThrownBy(() -> credentials.activity(match, a, key())).as("QR only after «Completar actividad»").isInstanceOf(BusinessRuleException.class);
        matches.confirm(match, a);
        var code = credentials.activity(match, a, key());
        assertThat(code.purpose()).isEqualTo("ACTIVITY");
        assertThat(code.manual()).startsWith("ACTIVIDAD-");
        assertThat(code.expiresAt()).isBetween(java.time.Instant.now().plusSeconds(19 * 60), java.time.Instant.now().plusSeconds(21 * 60));

        var waiting = scan(staff, qr(code));
        assertThat(waiting.result()).isEqualTo(CredentialService.ScanResult.WAITING);
        assertThat(waiting.missing()).containsExactly("Mario");
        assertThat(people.get(a).points()).isZero();

        matches.confirm(match, b);
        assertThat(matches.get(match, a).status()).as("both confirmed, still waiting for the scan").isEqualTo(MatchStatus.ACTIVE);
        assertThat(people.get(a).points()).isZero();
        assertThat(credentials.awaitingActivityScan(event)).isEqualTo(1);

        var credited = scan(staff, code.manual()); // the same code, typed: it stayed usable
        assertThat(credited.result()).isEqualTo(CredentialService.ScanResult.CREDITED);
        assertThat(matches.get(match, a).status()).isEqualTo(MatchStatus.COMPLETED);
        assertThat(people.get(a).points()).isEqualTo(20);
        assertThat(people.get(b).points()).isEqualTo(20);
        assertThat(grants(match)).as("one GRANT per person").isEqualTo(2);
        assertThat(credentials.awaitingActivityScan(event)).isZero();

        assertThat(scan(staff, qr(code)).result()).isEqualTo(CredentialService.ScanResult.ALREADY);
        assertThat(scan(role(event, AccountService.EventRole.ORGANIZER), code.manual()).result()).isEqualTo(CredentialService.ScanResult.ALREADY);
        assertThat(people.get(a).points()).isEqualTo(20);
        assertThat(people.get(b).points()).isEqualTo(20);
        assertThat(grants(match)).isEqualTo(2);
    }

    @Test void simultaneousScansOfBothCodesCreditOnce() throws Exception {
        verified(match, a, b);
        matches.confirm(match, a); matches.confirm(match, b);
        var codeA = credentials.activity(match, a, key()); var codeB = credentials.activity(match, b, key());
        long other = role(event, AccountService.EventRole.ORGANIZER);
        var pool = Executors.newFixedThreadPool(2); var start = new CountDownLatch(1);
        var jobs = List.of(pool.submit(() -> { start.await(); return scan(staff, qr(codeA)).result(); }),
                           pool.submit(() -> { start.await(); return scan(other, qr(codeB)).result(); }));
        start.countDown();
        var results = new ArrayList<CredentialService.ScanResult>(); for (var job : jobs) results.add(job.get(30, TimeUnit.SECONDS));
        pool.shutdown();
        assertThat(results).containsExactlyInAnyOrder(CredentialService.ScanResult.CREDITED, CredentialService.ScanResult.ALREADY);
        assertThat(people.get(a).points()).isEqualTo(20);
        assertThat(people.get(b).points()).isEqualTo(20);
        assertThat(grants(match)).isEqualTo(2);
        assertThat(sql.queryForObject("select count(*) from activity_qr_validations where match_id=?", Integer.class, match)).isEqualTo(1);
    }

    @Test void expiredOtherPurposeForeignEventAndOwnEncounterAreRejectedWithoutPoints() {
        verified(match, a, b);
        matches.confirm(match, a); matches.confirm(match, b);
        var code = credentials.activity(match, a, key());
        // Expired code.
        sql.update("update purpose_credentials set expires_at=? where id=?", java.sql.Timestamp.from(java.time.Instant.now().minusSeconds(60)), code.id());
        assertThatThrownBy(() -> scan(staff, qr(code))).isInstanceOf(BusinessRuleException.class).hasMessageContaining("caducó");
        SecurityContextHolder.clearContext();
        var fresh = credentials.activity(match, b, key());
        // Receipt and encounter codes are other purposes, rejected with a precise message.
        assertThatThrownBy(() -> scan(staff, "CANJE-ABCDEFGHJKLMNPQRSTUV")).isInstanceOf(BusinessRuleException.class).hasMessageContaining("recibo de canje");
        SecurityContextHolder.clearContext();
        assertThatThrownBy(() -> scan(staff, "PING:REDEMPTION:" + "a".repeat(43))).isInstanceOf(BusinessRuleException.class).hasMessageContaining("recibo de canje");
        SecurityContextHolder.clearContext();
        assertThatThrownBy(() -> scan(staff, "PING:ENCOUNTER:" + "a".repeat(43))).isInstanceOf(BusinessRuleException.class).hasMessageContaining("QR personal del encuentro");
        SecurityContextHolder.clearContext();
        // Staff of another event, and an account without any role.
        long foreign = role(event("Otro evento", 10, CheckMethod.BILATERAL), AccountService.EventRole.STAFF);
        assertThatThrownBy(() -> scan(foreign, qr(fresh))).isInstanceOf(AccessDeniedException.class);
        SecurityContextHolder.clearContext();
        assertThatThrownBy(() -> scan(account(), qr(fresh))).isInstanceOf(AccessDeniedException.class);
        SecurityContextHolder.clearContext();
        // A participant of this encounter who is also staff of the event.
        accounts.assign(general(), accountA, event, true, AccountService.EventRole.STAFF);
        assertThatThrownBy(() -> scan(accountA, qr(fresh))).isInstanceOf(AccessDeniedException.class).hasMessageContaining("Participas");
        SecurityContextHolder.clearContext();
        assertThat(people.get(a).points()).isZero();
        assertThat(people.get(b).points()).isZero();
        assertThat(matches.get(match, a).status()).isEqualTo(MatchStatus.ACTIVE);
        // The valid one still works afterwards.
        assertThat(scan(staff, qr(fresh)).result()).isEqualTo(CredentialService.ScanResult.CREDITED);
        assertThat(people.get(a).points()).isEqualTo(20);
    }

    @Test void encountersKeepTheMethodFrozenAtActivation() {
        // Activity switched to bilateral after activation: this encounter still needs the scan.
        verified(match, a, b);
        var form = config.activityForm(event, activity); form.setVerificationMethod(CheckMethod.BILATERAL); config.saveActivity(event, activity, form);
        matches.confirm(match, a); matches.confirm(match, b);
        assertThat(matches.get(match, a).status()).isEqualTo(MatchStatus.ACTIVE);
        assertThat(people.get(a).points()).isZero();
        // A new encounter, activated with the bilateral rule, completes by confirmations and is not scannable.
        long c = person(account(), event, "Carla"), d = person(account(), event, "Dario");
        long bilateral = matching.search(c); matches.accept(bilateral, c); matches.accept(bilateral, d); verified(bilateral, c, d);
        form = config.activityForm(event, activity); form.setVerificationMethod(CheckMethod.QR_VALIDATION); config.saveActivity(event, activity, form);
        matches.confirm(bilateral, c);
        assertThatThrownBy(() -> credentials.activity(bilateral, c, key())).isInstanceOf(BusinessRuleException.class);
        matches.confirm(bilateral, d);
        assertThat(matches.get(bilateral, c).status()).isEqualTo(MatchStatus.COMPLETED);
        assertThat(people.get(c).points()).isEqualTo(20);
    }

    @Test void zeroPointQrActivityCompletesWithoutCredit() {
        long zero = event("QR sin puntos", 0, CheckMethod.QR_VALIDATION);
        long x = person(account(), zero, "Ana"), y = person(account(), zero, "Beto");
        long m = matching.search(x); matches.accept(m, x); matches.accept(m, y); verified(m, x, y);
        matches.confirm(m, x); matches.confirm(m, y);
        var code = credentials.activity(m, y, key());
        assertThat(scan(role(zero, AccountService.EventRole.STAFF), qr(code)).result()).isEqualTo(CredentialService.ScanResult.CREDITED);
        assertThat(matches.get(m, x).status()).isEqualTo(MatchStatus.COMPLETED);
        assertThat(people.get(x).points()).isZero();
    }

    void login(TestBrowser browser, long account) throws Exception { browser.get("/login"); assertThat(browser.post("/login", Map.of("email", accounts.user(account).email(), "password", PASSWORD)).statusCode()).isEqualTo(302); }

    @Test void participantCompletesAndSeesTheQrStaffValidatesOverHttp() throws Exception {
        verified(match, a, b);
        try (var pa = new TestBrowser(port); var pb = new TestBrowser(port); var st = new TestBrowser(port); var nobody = new TestBrowser(port)) {
            login(pa, accountA); login(pb, accountB); login(st, staff); login(nobody, account());
            assertThat(pa.get("/matches/" + match).body()).contains("Completar actividad", "Validación presencial con QR");
            assertThat(pa.post("/matches/" + match + "/complete", Map.of("requestKey", key())).statusCode()).isEqualTo(302);
            String page = pa.get("/matches/" + match).body();
            assertThat(page).contains("/matches/" + match + "/activity-code.png", "ACTIVIDAD-", "Esperando a Mario");
            var png = pa.get("/matches/" + match + "/activity-code.png");
            assertThat(png.statusCode()).isEqualTo(200);
            assertThat(png.headers().firstValue("Content-Type").orElse("")).startsWith("image/png");
            String manual = page.replaceAll("(?s).*(ACTIVIDAD-[A-Z2-9]{20}).*", "$1");

            assertThat(nobody.get("/organizer/activity-scan").statusCode()).as("no role: denied").isEqualTo(403);
            assertThat(st.get("/organizer").body()).contains("Escanear QR de actividad");
            assertThat(st.get("/organizer/activity-scan").body()).contains("Escanear QR de actividad", "data-purpose=\"ACTIVITY\"");
            assertThat(st.post("/organizer/activity-scan", Map.of("credential", manual)).body()).contains("Falta que Mario pulse «Completar actividad»");

            assertThat(pb.get("/matches/" + match).body()).contains("Lina ya la completó");
            assertThat(pb.post("/matches/" + match + "/complete", Map.of("requestKey", key())).statusCode()).isEqualTo(302);
            st.get("/organizer/activity-scan");
            assertThat(st.post("/organizer/activity-scan", Map.of("credential", manual)).body()).contains("Actividad validada", "Se acreditaron 20 puntos a Lina y 20 a Mario");
            st.get("/organizer/activity-scan");
            assertThat(st.post("/organizer/activity-scan", Map.of("credential", manual)).body()).contains("ya estaba validada", "No se sumaron puntos otra vez");
            assertThat(people.get(a).points()).isEqualTo(20);
            assertThat(people.get(b).points()).isEqualTo(20);
        }
    }
}
