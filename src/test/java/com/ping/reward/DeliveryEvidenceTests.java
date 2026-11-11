package com.ping.reward;

import java.util.*;
import java.util.regex.Pattern;
import com.ping.auth.*;
import com.ping.encounter.*;
import com.ping.event.*;
import com.ping.match.*;
import com.ping.participant.*;
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

/** DEC-068: «Comprobar al entregar el beneficio». Points arrive once when both mark the activity; the receipt that uses them cannot be delivered before one in-person validation. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {"spring.datasource.url=jdbc:h2:mem:evidence;MODE=MySQL;DB_CLOSE_DELAY=-1", "spring.datasource.username=sa", "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=validate"})
@ActiveProfiles("verification-test")
class DeliveryEvidenceTests {
    @Autowired EventService events; @Autowired EncounterService config; @Autowired ParticipantService people; @Autowired MatchingService matching;
    @Autowired MatchService matches; @Autowired CredentialService credentials; @Autowired AccountService accounts; @Autowired RewardService rewards;
    @Autowired BenefitAdminService benefits; @Autowired JdbcTemplate sql; @Autowired PasswordEncoder encoder; @LocalServerPort int port;
    static final String PASSWORD = "Contraseña exclusiva de fixture2026";
    long event, a, b, match, accountA, accountB, benefit;

    @BeforeEach void setup() {
        var f = new EventForm(); f.setName("Evidencia ficticia " + UUID.randomUUID()); event = events.create(f).getId(); events.transition(event, EventStatus.OPEN);
        var p = new PointForm(); p.setName("Plazoleta"); p.setDirections("Junto a la fuente"); long point = config.savePoint(event, null, p);
        var act = new ActivityForm(); act.setTitle("Encuentra a la mascota de la U"); act.setInstructions("Búsquenla y tómense una foto juntos."); act.setRewardPoints(20); act.setPointIds(Set.of(point));
        act.setVerificationMethod(CheckMethod.AT_DELIVERY); act.setCheckInstructions("Una foto de los dos con la mascota");
        config.saveActivity(event, null, act);
        var bf = new BenefitForm(); bf.setName("Botella del evento"); bf.setDescription("Botella reutilizable"); bf.setPointsCost(10); bf.setClaimLocation("Mesa amarilla de entregas"); bf.setClaimInstructions("Junto a la entrada principal.");
        benefit = benefits.save(event, null, bf);
        accountA = account(); accountB = account(); a = person(accountA, event, "Persona A"); b = person(accountB, event, "Persona B");
        match = matching.search(a); matches.accept(match, a); matches.accept(match, b);
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    long account() { String email = UUID.randomUUID() + "@fixture.test"; sql.update("insert into ping_accounts(email,password_hash,email_verified) values(?,?,true)", email, encoder.encode(PASSWORD)); return accounts.find(email).orElseThrow(); }
    long person(long account, long event, String name) { var f = new ParticipantForm(); f.setAlias(name); f.setEventId(event); f.setAvailable(true); long id = people.create(f).id(); accounts.bind(account, id, event); return id; }
    long general() { String email = "general@fixture.test"; if (accounts.find(email).isEmpty()) accounts.bootstrap(email, PASSWORD); return accounts.find(email).orElseThrow(); }
    long staff(long forEvent) { long id = account(); accounts.assign(general(), id, forEvent, true, AccountService.EventRole.STAFF); return id; }
    void as(long id) { var user = accounts.user(id); SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities())); }
    String key() { return UUID.randomUUID().toString(); }
    void verified() {
        matches.arrive(match, a); matches.arrive(match, b);
        var ca = credentials.encounter(match, a, key()); var cb = credentials.encounter(match, b, key());
        credentials.verify(match, a, cb.manual(), "fixture"); credentials.verify(match, b, ca.manual(), "fixture");
    }
    void bothMarked() { verified(); matches.confirm(match, a); matches.confirm(match, b); }
    int count(String table, String where, Object... args) { return sql.queryForObject("select count(*) from " + table + " where " + where, Integer.class, args); }

    @Test void markingDoneCreditsEachPersonOnceAsUncheckedPoints() {
        assertThat(matches.get(match, a).atDelivery()).isTrue();
        assertThatThrownBy(() -> matches.confirm(match, a)).as("still needs arrivals and codes").isInstanceOf(BusinessRuleException.class);
        verified();
        matches.confirm(match, a); matches.confirm(match, a);
        assertThat(matches.get(match, a).status()).isEqualTo(MatchStatus.ACTIVE);
        assertThat(people.get(a).points()).as("one mark is not enough").isZero();
        matches.confirm(match, b); matches.confirm(match, b); matches.confirm(match, a);
        assertThat(matches.get(match, a).status()).isEqualTo(MatchStatus.COMPLETED);
        assertThat(people.get(a).points()).isEqualTo(20); assertThat(people.get(b).points()).isEqualTo(20);
        assertThat(sql.queryForObject("select attempt from match_completion_sagas where match_id=?", Integer.class, match)).isEqualTo(1);
        assertThat(count("evidence_credits", "match_id=?", match)).isEqualTo(2);
        assertThat(rewards.uncheckedPoints(a)).isEqualTo(20); assertThat(rewards.uncheckedPoints(b)).isEqualTo(20);
    }

    @Test void receiptNeedsInPersonValidationAndOneValidationCoversThePartner() {
        bothMarked();
        String requestKey = key(); long claim = rewards.request(a, benefit, requestKey);
        assertThat(rewards.request(a, benefit, requestKey)).as("retry with the same key").isEqualTo(claim);
        assertThat(people.get(a).points()).isEqualTo(10);
        assertThat(sql.queryForObject("select amount from claim_evidence where claim_id=? and match_id=?", Integer.class, claim, match)).isEqualTo(10);
        String code = credentials.redemption(claim, a, key()).manual();
        long staffer = staff(event); as(staffer);
        var found = credentials.lookup(code, "fixture");
        assertThat(found.evidenceMissing()).isTrue();
        assertThat(found.evidence()).singleElement().satisfies(r -> { assertThat(r.activityTitle()).isEqualTo("Encuentra a la mascota de la U"); assertThat(r.evidence()).isEqualTo("Una foto de los dos con la mascota"); });
        // Scanning identifies the receipt; it does not deliver without the evidence.
        assertThatThrownBy(() -> credentials.deliver(code, ClaimStatus.DELIVERED, "fixture")).isInstanceOf(BusinessRuleException.class).hasMessageContaining("evidencia");
        // Evidence missing: a note, the receipt stays pending and nothing is charged.
        credentials.evidenceMissing(code, "Vuelve con la foto de la mascota.", "fixture");
        assertThat(credentials.lookup(code, "fixture").note().note()).isEqualTo("Vuelve con la foto de la mascota.");
        assertThat(sql.queryForObject("select status from reward_claims where id=?", String.class, claim)).isEqualTo("REQUESTED");
        assertThat(people.get(a).points()).isEqualTo(10);
        // Validation: once, by whom and when; it grants nothing.
        assertThat(credentials.validateEvidence(code, match, "fixture")).isTrue();
        assertThat(credentials.validateEvidence(code, match, "fixture")).as("repeat is a no-op").isFalse();
        assertThat(sql.queryForObject("select validated_by from evidence_validations where match_id=?", Long.class, match)).isEqualTo(staffer);
        assertThat(sql.queryForObject("select validated_at from evidence_validations where match_id=?", java.sql.Timestamp.class, match)).isNotNull();
        assertThat(people.get(a).points()).isEqualTo(10); assertThat(people.get(b).points()).isEqualTo(20);
        credentials.deliver(code, ClaimStatus.DELIVERED, "fixture"); credentials.deliver(code, ClaimStatus.DELIVERED, "fixture");
        assertThat(sql.queryForObject("select status from reward_claims where id=?", String.class, claim)).isEqualTo("DELIVERED");
        assertThat(people.get(a).points()).as("no second debit").isEqualTo(10);
        assertThat(count("auth_audit", "action='DELIVER_CLAIM' and subject_id=?", claim)).isEqualTo(1);
        assertThat(count("auth_audit", "action='VALIDATE_EVIDENCE' and subject_id=?", match)).isEqualTo(1);
        // The partner does not show the same photo again.
        SecurityContextHolder.clearContext();
        assertThat(rewards.uncheckedPoints(b)).isZero();
        long partnerClaim = rewards.request(b, benefit, key());
        String partnerCode = credentials.redemption(partnerClaim, b, key()).manual();
        as(staffer);
        var partnerLookup = credentials.lookup(partnerCode, "fixture");
        assertThat(partnerLookup.evidenceMissing()).isFalse();
        assertThat(partnerLookup.evidence()).as("staff sees the challenge was already validated").singleElement().satisfies(r -> assertThat(r.validated()).isTrue());
        credentials.deliver(partnerCode, ClaimStatus.DELIVERED, "fixture");
        assertThat(people.get(b).points()).isEqualTo(10);
    }

    @Test void partnerReceiptPaidBeforeValidationShowsItAsAlreadyValidated() {
        bothMarked();
        long claimA = rewards.request(a, benefit, key()), claimB = rewards.request(b, benefit, key());
        String codeA = credentials.redemption(claimA, a, key()).manual(), codeB = credentials.redemption(claimB, b, key()).manual();
        as(staff(event));
        credentials.validateEvidence(codeA, match, "fixture");
        var partner = credentials.lookup(codeB, "fixture");
        assertThat(partner.evidenceMissing()).isFalse();
        assertThat(partner.evidence()).singleElement().satisfies(r -> assertThat(r.validated()).isTrue());
        credentials.deliver(codeB, ClaimStatus.DELIVERED, "fixture");
    }

    @Test void checkedPointsAreSpentFirstAndOnlyTheUncheckedPartNeedsEvidence() {
        sql.update("update participants set points=points+15 where id=?", a); // earlier points from a bilateral activity
        bothMarked();
        assertThat(people.get(a).points()).isEqualTo(35); assertThat(rewards.uncheckedPoints(a)).isEqualTo(20);
        long first = rewards.request(a, benefit, key());
        assertThat(count("claim_evidence", "claim_id=?", first)).as("paid only with checked points: normal flow").isZero();
        long second = rewards.request(a, benefit, key());
        assertThat(sql.queryForObject("select amount from claim_evidence where claim_id=?", Integer.class, second)).isEqualTo(5);
        assertThat(rewards.uncheckedPoints(a)).isEqualTo(15);
        assertThat(people.get(a).points()).isEqualTo(15);
        long third = rewards.request(a, benefit, key());
        assertThat(sql.queryForObject("select amount from claim_evidence where claim_id=?", Integer.class, third)).isEqualTo(10);
        assertThat(people.get(a).points()).isEqualTo(5); assertThat(rewards.uncheckedPoints(a)).isEqualTo(5);
        assertThatThrownBy(() -> rewards.request(a, benefit, key())).as("the same balance cannot pay two receipts").isInstanceOf(BusinessRuleException.class);
        String code = credentials.redemption(first, a, key()).manual();
        as(staff(event)); credentials.deliver(code, ClaimStatus.DELIVERED, "fixture");
    }

    @Test void onlyStaffOfTheEventWhoIsNotInTheEncounterCanValidate() {
        bothMarked();
        long claim = rewards.request(a, benefit, key()); String code = credentials.redemption(claim, a, key()).manual();
        var other = new EventForm(); other.setName("Otro evento " + UUID.randomUUID()); long otherEvent = events.create(other).getId();
        as(staff(otherEvent));
        assertThatThrownBy(() -> credentials.validateEvidence(code, match, "fixture")).isInstanceOf(AccessDeniedException.class);
        accounts.assign(general(), accountB, event, true, AccountService.EventRole.STAFF); as(accountB);
        assertThatThrownBy(() -> credentials.validateEvidence(code, match, "fixture")).as("the partner cannot validate their own encounter").isInstanceOf(AccessDeniedException.class);
        assertThat(count("evidence_validations", "match_id=?", match)).isZero();
    }

    @Test void httpJourneyShowsWhatToBringAndBlocksDeliveryUntilValidated() throws Exception {
        verified();
        try (var aa = new TestBrowser(port); var bb = new TestBrowser(port); var staffer = new TestBrowser(port)) {
            for (var pair : List.of(Map.entry(aa, accountA), Map.entry(bb, accountB), Map.entry(staffer, staff(event)))) {
                pair.getKey().get("/login");
                assertThat(pair.getKey().post("/login", Map.of("email", accounts.user(pair.getValue()).email(), "password", PASSWORD)).statusCode()).isEqualTo(302);
            }
            aa.get("/me");
            String page = aa.get("/matches/" + match).body();
            assertThat(page).contains("Marcar actividad como hecha", "Comprobar al entregar el beneficio", "Una foto de los dos con la mascota", "+20 puntos");
            aa.post("/matches/" + match + "/confirm", Map.of());
            assertThat(aa.get("/matches/" + match).body()).contains("Esperando a tu compañero").doesNotContain("Foto validada");
            assertThat(aa.get("/events/" + event + "/plan").body()).contains("Se comprueba al recoger el premio", "Una foto de los dos con la mascota");
            bb.get("/me"); bb.get("/matches/" + match); bb.post("/matches/" + match + "/confirm", Map.of());
            assertThat(people.get(a).points()).isEqualTo(20);
            String completed = aa.get("/matches/" + match).body();
            assertThat(completed).contains("Ya puedes canjear estos puntos").doesNotContain("Foto validada", "Premio entregado");
            String benefitsPage = aa.get("/me/benefits").body();
            assertThat(benefitsPage).contains("20 puntos se comprueban al entregar", "Usa puntos por comprobar");
            var paid = aa.post("/me/benefits", Map.of("benefitId", Long.toString(benefit), "requestKey", key()));
            assertThat(paid.statusCode()).isEqualTo(302);
            String receipt = aa.get(paid.headers().firstValue("Location").orElseThrow()).body();
            assertThat(receipt).contains("Lleva la evidencia de tu actividad", "Una foto de los dos con la mascota", "Mesa amarilla de entregas").doesNotContain("Comprobada el");
            var manual = Pattern.compile("CANJE-[A-Z2-9]{20}").matcher(receipt); assertThat(manual.find()).isTrue(); String code = manual.group();
            staffer.get("/organizer/redemptions");
            String lookup = staffer.post("/organizer/redemptions/lookup", Map.of("credential", code)).body();
            assertThat(lookup).contains("Antes de entregar: comprueba la evidencia", "Encuentra a la mascota de la U", "La vi y corresponde", "Se habilita cuando registres la evidencia");
            String refused = staffer.post("/organizer/redemptions/deliver", Map.of("credential", code, "status", "DELIVERED")).body();
            assertThat(refused).contains("Comprueba primero la evidencia");
            String missing = staffer.post("/organizer/redemptions/evidence-missing", Map.of("credential", code, "note", "Vuelve con la foto, por favor.")).body();
            assertThat(missing).contains("Incidencia registrada. El recibo sigue pendiente");
            assertThat(aa.get(paid.headers().firstValue("Location").orElseThrow()).body()).contains("La persona encargada indicó: Vuelve con la foto, por favor.");
            String validated = staffer.post("/organizer/redemptions/evidence", Map.of("credential", code, "matchId", Long.toString(match))).body();
            assertThat(validated).contains("Evidencia comprobada", "Ya validada");
            assertThat(staffer.post("/organizer/redemptions/deliver", Map.of("credential", code, "status", "DELIVERED")).statusCode()).isEqualTo(302);
            assertThat(aa.get(paid.headers().firstValue("Location").orElseThrow()).body()).contains("La entrega ya está registrada");
            assertThat(people.get(a).points()).isEqualTo(10);
        }
    }
}
