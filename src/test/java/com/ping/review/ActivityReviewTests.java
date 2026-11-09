package com.ping.review;

import java.util.*;
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

/** DEC-066: staff review delays the existing completion; it never grants points itself. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {"spring.datasource.url=jdbc:h2:mem:review;MODE=MySQL;DB_CLOSE_DELAY=-1", "spring.datasource.username=sa", "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=validate"})
@ActiveProfiles("verification-test")
class ActivityReviewTests {
    @Autowired EventService events; @Autowired EncounterService config; @Autowired ParticipantService people; @Autowired MatchingService matching;
    @Autowired MatchService matches; @Autowired CredentialService credentials; @Autowired AccountService accounts; @Autowired ActivityReviewService reviews;
    @Autowired JdbcTemplate sql; @Autowired PasswordEncoder encoder; @LocalServerPort int port;
    static final String PASSWORD = "Contraseña exclusiva de fixture2026";
    long event, activity, a, b, match, accountA, accountB;

    @BeforeEach void setup() {
        var f = new EventForm(); f.setName("Revisión ficticia " + UUID.randomUUID()); event = events.create(f).getId(); events.transition(event, EventStatus.OPEN);
        var p = new PointForm(); p.setName("Patio público"); p.setDirections("Junto al letrero"); long point = config.savePoint(event, null, p);
        var act = new ActivityForm(); act.setTitle("Canciones"); act.setInstructions("Recomiéndense una canción."); act.setRewardPoints(25); act.setPointIds(Set.of(point));
        act.setVerificationMethod(CheckMethod.STAFF_REVIEW); act.setCheckInstructions("Que nombren ambas canciones."); act.setResponsePrompt("¿Qué canciones eligieron?");
        activity = config.saveActivity(event, null, act);
        accountA = account(); accountB = account(); a = person(accountA, event, "Persona A"); b = person(accountB, event, "Persona B");
        match = matching.search(a); matches.accept(match, a); matches.accept(match, b);
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    long account() { String email = UUID.randomUUID() + "@fixture.test"; sql.update("insert into ping_accounts(email,password_hash,email_verified) values(?,?,true)", email, encoder.encode(PASSWORD)); return accounts.find(email).orElseThrow(); }
    long person(long account, long event, String name) { var f = new ParticipantForm(); f.setAlias(name); f.setEventId(event); f.setAvailable(true); long id = people.create(f).id(); accounts.bind(account, id, event); return id; }
    long general() { String email = "general@fixture.test"; if (accounts.find(email).isEmpty()) accounts.bootstrap(email, PASSWORD); return accounts.find(email).orElseThrow(); }
    long staff(long forEvent) { long id = account(); accounts.assign(general(), id, forEvent, true); return id; }
    void as(long id) { var user = accounts.user(id); SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities())); }
    String key() { return UUID.randomUUID().toString(); }
    void verified() {
        matches.arrive(match, a); matches.arrive(match, b);
        var ca = credentials.encounter(match, a, key()); var cb = credentials.encounter(match, b, key());
        credentials.verify(match, a, cb.manual(), "fixture"); credentials.verify(match, b, ca.manual(), "fixture");
    }
    void approve() { assertThat(reviews.decide(event, match, true, "")).isTrue(); SecurityContextHolder.clearContext(); matches.completeAfterReview(match); }

    @Test void methodIsFrozenAtActivationAndNeverBlocksEarlierEncounters() {
        assertThat(matches.get(match, a).staffReview()).isTrue();
        var form = config.activityForm(event, activity); form.setVerificationMethod(CheckMethod.BILATERAL); config.saveActivity(event, activity, form);
        assertThat(matches.get(match, a).staffReview()).as("snapshot taken on activation").isTrue();
        // An encounter activated with the bilateral rule keeps completing by confirmations even if the activity later requires review.
        long c = person(account(), event, "Persona C"), d = person(account(), event, "Persona D");
        long other = matching.search(c); matches.accept(other, c); matches.accept(other, d);
        form = config.activityForm(event, activity); form.setVerificationMethod(CheckMethod.STAFF_REVIEW); form.setCheckInstructions("Revisar"); config.saveActivity(event, activity, form);
        matches.arrive(other, c); matches.arrive(other, d);
        var cc = credentials.encounter(other, c, key()); var cd = credentials.encounter(other, d, key());
        credentials.verify(other, c, cd.manual(), "fixture"); credentials.verify(other, d, cc.manual(), "fixture");
        matches.confirm(other, c); matches.confirm(other, d);
        assertThat(matches.get(other, c).status()).isEqualTo(MatchStatus.COMPLETED);
        assertThat(people.get(c).points()).isEqualTo(25);
    }

    @Test void confirmationsAloneDoNotCreditAndApprovalCreditsExactlyOnce() {
        assertThatThrownBy(() -> reviews.submit(match, a, "Antes de verificar")).isInstanceOf(BusinessRuleException.class);
        verified();
        assertThatThrownBy(() -> reviews.submit(match, a, "  ")).isInstanceOf(BusinessRuleException.class);
        reviews.submit(match, a, "Clocks y Yellow"); reviews.submit(match, a, "Clocks y Yellow");
        assertThat(reviews.get(match).revision()).as("identical resubmission is a no-op").isZero();
        matches.confirm(match, a); matches.confirm(match, b); matches.confirm(match, b);
        assertThat(matches.get(match, a).status()).isEqualTo(MatchStatus.ACTIVE);
        assertThat(people.get(a).points()).isZero(); assertThat(people.get(b).points()).isZero();
        as(staff(event)); approve();
        as(staff(event)); approve(); matches.confirm(match, a);
        assertThat(matches.get(match, a).status()).isEqualTo(MatchStatus.COMPLETED);
        assertThat(people.get(a).points()).isEqualTo(25); assertThat(people.get(b).points()).isEqualTo(25);
        assertThat(sql.queryForObject("select attempt from match_completion_sagas where match_id=?", Integer.class, match)).isEqualTo(1);
    }

    @Test void approvalBeforeConfirmationsCreditsWhenTheSecondPersonConfirms() {
        verified(); reviews.submit(match, b, "Clocks y Yellow");
        as(staff(event)); approve();
        assertThat(people.get(a).points()).isZero();
        matches.confirm(match, a); assertThat(people.get(a).points()).isZero();
        matches.confirm(match, b);
        assertThat(people.get(a).points()).isEqualTo(25); assertThat(people.get(b).points()).isEqualTo(25);
    }

    @Test void correctionNeedsCommentAndApprovedTaskCannotBeReopened() {
        verified(); reviews.submit(match, a, "Solo una canción");
        as(staff(event));
        assertThatThrownBy(() -> reviews.decide(event, match, false, " ")).isInstanceOf(BusinessRuleException.class);
        assertThat(reviews.decide(event, match, false, "Falta la segunda canción")).isFalse();
        assertThat(reviews.get(match).status()).isEqualTo(ActivityReviewService.Status.CHANGES);
        reviews.submit(match, b, "Clocks y Yellow");
        assertThat(reviews.get(match).status()).isEqualTo(ActivityReviewService.Status.PENDING);
        assertThat(reviews.decide(event, match, true, "")).isTrue();
        assertThatThrownBy(() -> reviews.decide(event, match, false, "Otra cosa")).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> reviews.submit(match, a, "Cambio")).isInstanceOf(BusinessRuleException.class);
    }

    @Test void onlyAuthorisedStaffOfTheSameEventWhoIsNotInTheEncounterCanReview() {
        verified(); reviews.submit(match, a, "Clocks y Yellow");
        var other = new EventForm(); other.setName("Otro evento " + UUID.randomUUID()); long otherEvent = events.create(other).getId();
        as(staff(otherEvent));
        assertThatThrownBy(() -> reviews.forEvent(event)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> reviews.decide(event, match, true, "")).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> reviews.decide(otherEvent, match, true, "")).isInstanceOf(NotFoundException.class);
        accounts.assign(general(), accountA, event, true); as(accountA);
        assertThat(reviews.forEvent(event)).singleElement().satisfies(item -> assertThat(item.ownEncounter()).isTrue());
        assertThatThrownBy(() -> reviews.decide(event, match, true, "")).isInstanceOf(AccessDeniedException.class);
        as(accountB); assertThatThrownBy(() -> reviews.forEvent(event)).isInstanceOf(AccessDeniedException.class);
        assertThat(people.get(a).points()).isZero();
    }

    @Test void httpSubmissionIsOwnedAndStaffPageIsProtected() throws Exception {
        verified();
        try (var aa = new TestBrowser(port); var bb = new TestBrowser(port); var stranger = new TestBrowser(port); var staffer = new TestBrowser(port)) {
            for (var pair : List.of(Map.entry(aa, accountA), Map.entry(bb, accountB), Map.entry(staffer, staff(event)), Map.entry(stranger, account()))) {
                pair.getKey().get("/login");
                assertThat(pair.getKey().post("/login", Map.of("email", accounts.user(pair.getValue()).email(), "password", PASSWORD)).statusCode()).isEqualTo(302);
            }
            aa.get("/me");
            String page = aa.get("/matches/" + match).body();
            assertThat(page).contains("Hagan la actividad y envíenla", "Revisión del personal", "¿Qué canciones eligieron?", "Que nombren ambas canciones.");
            aa.post("/matches/" + match + "/review", Map.of("response", "Clocks y Yellow"));
            assertThat(reviews.get(match).response()).isEqualTo("Clocks y Yellow");
            assertThat(stranger.get("/events/" + event + "/reviews").statusCode()).isEqualTo(403);
            bb.get("/me"); assertThat(bb.get("/events/" + event + "/reviews").statusCode()).isEqualTo(403);
            String list = staffer.get("/events/" + event + "/reviews").body();
            assertThat(list).contains("Clocks y Yellow", "Pendiente de revisión", "Aprobar");
            assertThat(staffer.post("/events/" + event + "/reviews/" + match, Map.of("decision", "approve")).statusCode()).isEqualTo(302);
            assertThat(reviews.get(match).status()).isEqualTo(ActivityReviewService.Status.APPROVED);
            assertThat(people.get(a).points()).as("approval without confirmations grants nothing").isZero();
        }
    }
}
