package com.ping.roster;

import java.util.*;
import com.ping.auth.*;
import com.ping.encounter.*;
import com.ping.event.*;
import com.ping.match.*;
import com.ping.participant.*;
import com.ping.reward.*;
import com.ping.support.TestBrowser;
import com.ping.verification.CredentialService;
import com.ping.web.*;
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

/** DEC-068: withdraw and readmit participants of one event. Organisation of that event or general administration only. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {"spring.datasource.url=jdbc:h2:mem:roster;MODE=MySQL;DB_CLOSE_DELAY=-1", "spring.datasource.username=sa", "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=validate"})
@ActiveProfiles("verification-test")
class EventParticipantsTests {
    @Autowired EventService events; @Autowired EncounterService config; @Autowired ParticipantService people; @Autowired MatchingService matching;
    @Autowired MatchService matches; @Autowired CredentialService credentials; @Autowired AccountService accounts; @Autowired RewardService rewards;
    @Autowired BenefitAdminService benefits; @Autowired EventParticipantsService roster; @Autowired JdbcTemplate sql; @Autowired PasswordEncoder encoder; @LocalServerPort int port;
    static final String PASSWORD = "Contraseña exclusiva de fixture2026";
    long event, otherEvent, c, d, accountC, accountD, benefit;

    @BeforeEach void setup() {
        event = event("Retiro ficticio"); otherEvent = event("Otro evento");
        var bf = new BenefitForm(); bf.setName("Pin del evento"); bf.setPointsCost(10); bf.setClaimLocation("Mesa azul"); bf.setClaimInstructions("Junto a la entrada.");
        benefit = benefits.save(event, null, bf);
        accountC = account(); accountD = account(); c = person(accountC, event, "Camila"); d = person(accountD, event, "Dante");
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    long event(String name) {
        var f = new EventForm(); f.setName(name + " " + UUID.randomUUID()); long id = events.create(f).getId(); events.transition(id, EventStatus.OPEN);
        var p = new PointForm(); p.setName("Patio"); p.setDirections("Junto al letrero"); long point = config.savePoint(id, null, p);
        var act = new ActivityForm(); act.setTitle("Canciones"); act.setInstructions("Recomiéndense una canción."); act.setRewardPoints(15); act.setPointIds(Set.of(point)); config.saveActivity(id, null, act);
        return id;
    }
    long account() { String email = UUID.randomUUID() + "@fixture.test"; sql.update("insert into ping_accounts(email,password_hash,email_verified) values(?,?,true)", email, encoder.encode(PASSWORD)); return accounts.find(email).orElseThrow(); }
    long person(long account, long event, String name) { var f = new ParticipantForm(); f.setAlias(name); f.setEventId(event); f.setAvailable(true); long id = people.create(f).id(); accounts.bind(account, id, event); return id; }
    long general() { String email = "general@fixture.test"; if (accounts.find(email).isEmpty()) accounts.bootstrap(email, PASSWORD); return accounts.find(email).orElseThrow(); }
    long admin(long forEvent, AccountService.EventRole role) { long id = account(); accounts.assign(general(), id, forEvent, true, role); return id; }
    void as(long id) { var user = accounts.user(id); SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities())); }
    String key() { return UUID.randomUUID().toString(); }

    @Test void withdrawalCancelsTheEncounterAndPendingReceiptsAndKeepsEverythingElse() {
        long match = matching.search(c); matches.accept(match, c); matches.accept(match, d);
        sql.update("update participants set points=25 where id=?", c);
        long claim = rewards.request(c, benefit, key()); String code = credentials.redemption(claim, c, key()).manual();
        long elsewhere = person(accountC, otherEvent, "Camila");
        long organizer = admin(event, AccountService.EventRole.ORGANIZER); as(organizer);
        assertThat(roster.search(event, "cam")).extracting(EventParticipantsService.Row::alias).containsExactly("Camila");
        assertThat(roster.search(event, "%")).as("LIKE wildcards are literal").isEmpty();
        assertThat(roster.search(event, "_")).isEmpty();
        assertThatThrownBy(() -> roster.withdraw(event, c, " ")).as("reason required").isInstanceOf(BusinessRuleException.class);
        var done = roster.withdraw(event, c, "Reporte de la mesa de ayuda");
        assertThat(done.encounters()).isEqualTo(1); assertThat(done.claims()).isEqualTo(1);
        assertThat(roster.withdraw(event, c, "Otra vez").claims()).as("repeat changes nothing").isZero();
        assertThat(sql.queryForObject("select count(*) from participant_withdrawals where participant_id=?", Integer.class, c)).isEqualTo(1);
        assertThat(sql.queryForObject("select actor_id from participant_withdrawals where participant_id=?", Long.class, c)).isEqualTo(organizer);
        assertThat(sql.queryForObject("select reason from participant_withdrawals where participant_id=?", String.class, c)).isEqualTo("Reporte de la mesa de ayuda");
        // Partner: cancellation by the organisation, never the private reason.
        var partnerView = matches.get(match, d);
        assertThat(partnerView.status()).isEqualTo(MatchStatus.CANCELLED); assertThat(partnerView.cancelledByOrganization()).isTrue();
        assertThat(sql.queryForObject("select count(*) from match_reservations where participant_id in (?,?)", Integer.class, c, d)).isZero();
        // Pending receipt: cancelled, code revoked, cost back once. Account, points and other events are kept.
        assertThat(sql.queryForObject("select status from reward_claims where id=?", String.class, claim)).isEqualTo("CANCELLED");
        assertThat(people.get(c).points()).isEqualTo(25);
        as(admin(event, AccountService.EventRole.STAFF));
        assertThatThrownBy(() -> credentials.deliver(code, ClaimStatus.DELIVERED, "fixture")).isInstanceOf(BusinessRuleException.class);
        assertThat(people.withdrawn(elsewhere)).isFalse();
        assertThat(accounts.participant(accountC, event)).contains(c);
        // Not found when searching, cannot search, change availability or redeem while withdrawn.
        assertThatThrownBy(() -> matching.search(d)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> matching.search(c)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> rewards.request(c, benefit, key())).isInstanceOf(BusinessRuleException.class);
        // Readmission: same participation, points intact; they mark availability again to be found.
        as(organizer); roster.readmit(event, c); roster.readmit(event, c);
        assertThat(people.withdrawn(c)).isFalse(); assertThat(people.get(c).points()).isEqualTo(25);
        assertThat(sql.queryForObject("select count(*) from participant_withdrawals where participant_id=? and action='READMIT'", Integer.class, c)).isEqualTo(1);
        long e = person(account(), event, "Elena");
        sql.update("update participants set available=true, waiting_since=current_timestamp where id=?", c);
        assertThat(matching.search(e)).isPositive();
    }

    @Test void onlyTheOrganisationOfThatEventOrGeneralAdministrationCanWithdraw() {
        as(admin(otherEvent, AccountService.EventRole.ORGANIZER));
        assertThatThrownBy(() -> roster.withdraw(event, c, "Motivo de prueba")).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> roster.search(event, "")).isInstanceOf(AccessDeniedException.class);
        as(admin(event, AccountService.EventRole.STAFF));
        assertThatThrownBy(() -> roster.withdraw(event, c, "Motivo de prueba")).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> roster.readmit(event, c)).isInstanceOf(AccessDeniedException.class);
        as(admin(event, AccountService.EventRole.ORGANIZER));
        assertThatThrownBy(() -> roster.withdraw(otherEvent, c, "Motivo de prueba")).isInstanceOf(AccessDeniedException.class);
        as(accountD);
        assertThatThrownBy(() -> roster.withdraw(event, c, "Motivo de prueba")).isInstanceOf(AccessDeniedException.class);
        as(general());
        roster.withdraw(event, c, "Motivo de prueba");
        assertThat(people.withdrawn(c)).isTrue();
    }

    @Test void httpPermissionsAndTheWithdrawnPersonCannotComeBackWithTheCodeOrQr() throws Exception {
        long organizer = admin(event, AccountService.EventRole.ORGANIZER), staff = admin(event, AccountService.EventRole.STAFF), foreign = admin(otherEvent, AccountService.EventRole.ORGANIZER);
        String entry = events.get(event).getEntryCode();
        try (var org = new TestBrowser(port); var st = new TestBrowser(port); var fo = new TestBrowser(port); var cc = new TestBrowser(port)) {
            for (var pair : List.of(Map.entry(org, organizer), Map.entry(st, staff), Map.entry(fo, foreign), Map.entry(cc, accountC))) {
                pair.getKey().get("/login");
                assertThat(pair.getKey().post("/login", Map.of("email", accounts.user(pair.getValue()).email(), "password", PASSWORD)).statusCode()).isEqualTo(302);
            }
            assertThat(org.get("/organizer?event=" + event).body()).contains("Participantes");
            String list = org.get("/events/" + event + "/participants?q=cam").body();
            assertThat(list).contains("Camila", "Retirar del evento").doesNotContain("Dante");
            String confirm = org.get("/events/" + event + "/participants/" + c + "/withdraw").body();
            assertThat(confirm).contains("Vas a retirar a «Camila» de «Retiro ficticio");
            // Staff (review/delivery only) and another event's organisation: refused by the server.
            assertThat(st.get("/events/" + event + "/participants").statusCode()).isEqualTo(403);
            assertThat(st.post("/events/" + event + "/participants/" + c + "/withdraw", Map.of("reason", "Motivo de prueba")).statusCode()).isEqualTo(403);
            assertThat(st.get("/events/" + event + "/encounters").statusCode()).isEqualTo(403);
            assertThat(st.get("/events/" + event + "/reviews").statusCode()).isEqualTo(200);
            assertThat(st.get("/organizer?event=" + event).body()).doesNotContain("Busca por alias");
            assertThat(fo.get("/events/" + event + "/participants").statusCode()).isEqualTo(403);
            assertThat(people.withdrawn(c)).isFalse();
            cc.get("/me"); assertThat(cc.get("/me").statusCode()).isEqualTo(200);
            org.get("/events/" + event + "/participants/" + c + "/withdraw");
            assertThat(org.post("/events/" + event + "/participants/" + c + "/withdraw", Map.of("reason", "Reporte de la mesa de ayuda")).statusCode()).isEqualTo(302);
            assertThat(people.withdrawn(c)).isTrue();
            assertThat(org.get("/events/" + event + "/participants?q=Camila").body()).contains("Fuera del evento", "Readmitir");
            // The withdrawn person: out of participant pages, and the same code or QR does not let them back in.
            var me = cc.get("/me"); assertThat(me.statusCode()).isEqualTo(302); assertThat(me.headers().firstValue("Location").orElseThrow()).contains("/join?retirado=1");
            var back = cc.get("/join?code=" + entry);
            assertThat(back.statusCode()).isEqualTo(403); assertThat(back.body()).contains("La organización de ese evento te retiró");
            assertThat(cc.get("/me").statusCode()).isEqualTo(302);
            assertThat(cc.get("/me/state").statusCode()).isEqualTo(401);
            org.get("/events/" + event + "/participants?q=Camila");
            assertThat(org.post("/events/" + event + "/participants/" + c + "/readmit", Map.of()).statusCode()).isEqualTo(302);
            assertThat(cc.get("/join?code=" + entry).statusCode()).isEqualTo(302);
            assertThat(cc.get("/me").statusCode()).isEqualTo(200);
        }
    }
}
