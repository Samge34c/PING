package com.ping.reward;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import com.ping.demo.DemoPreparationService;
import com.ping.participant.*;
import com.ping.match.*;
import com.ping.support.TestBrowser;
import com.ping.web.*;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "spring.datasource.url=jdbc:h2:mem:rewards;MODE=MySQL;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class RewardTests {
    @Autowired RewardService rewards;
    @Autowired RewardClaimRepository claims;
    @Autowired DemoPreparationService preparation;
    @Autowired ParticipantService participants;
    @Autowired MatchingService matching;
    @Autowired MatchService matches;
    @Autowired MatchCompletionSagaSteps steps;
    @Autowired MatchCompletionSaga saga;
    @Autowired JdbcTemplate sql;
    @LocalServerPort int port;
    long a,b,event,benefit;
    @BeforeEach void prepare() {
        sql.update("update demo_scenarios set event_id = null"); sql.update("delete from feedback");
        sql.update("delete from match_reservations"); sql.update("delete from ping_matches");
        sql.update("delete from participant_interests"); sql.update("delete from participants"); sql.update("delete from events");
        sql.update("update benefits set active=true, points_cost=20");
        var s=preparation.prepare(); event=s.eventId(); a=s.people().get(0).id(); b=s.people().get(1).id();
        benefit=rewards.catalog().getFirst().getId();
        for(long id:new long[]{a,b}) { AvailabilityForm f=new AvailabilityForm(); f.setAvailable(true); f.setVersion(participants.get(id).version()); participants.setAvailability(id,f); }
        complete(); complete();
        assertThat(participants.get(a).points()).isEqualTo(20);
    }
    long active() { long m=matching.search(a); matches.accept(m,a); matches.accept(m,b); matches.arrive(m,a); matches.arrive(m,b); return m; }
    void complete() { long m=active(); matches.confirm(m,a); matches.confirm(m,b); }
    String key() { return UUID.randomUUID().toString(); }
    @Test void exactBalanceClaimIsIdempotentAndDoesNotAlterIssuedPointsOrHistory() {
        String key=key(); long first=rewards.request(a,benefit,key);
        assertThat(rewards.request(a,benefit,key.toUpperCase(Locale.ROOT))).isEqualTo(first);
        assertThat(participants.get(a).points()).isZero(); assertThat(participants.get(b).points()).isEqualTo(20);
        assertThat(claims.count()).isEqualTo(1); assertThat(matches.list(a)).hasSize(2);
        assertThat(rewards.personal(a)).singleElement().satisfies(c -> { assertThat(c.cost()).isEqualTo(20); assertThat(c.status()).isEqualTo(ClaimStatus.REQUESTED); });
        assertThat(rewards.forEvent(event).requested()).isEqualTo(1);
    }
    @Test void insufficientBalanceAndInvalidKeysNeverDebitOrCreateClaims() {
        assertThatThrownBy(() -> rewards.request(a,benefit,"invalid")).isInstanceOf(BusinessRuleException.class);
        assertThat(participants.get(a).points()).isEqualTo(20);
        rewards.request(a,benefit,key());
        assertThatThrownBy(() -> rewards.request(a,benefit,key())).isInstanceOf(BusinessRuleException.class);
        assertThat(participants.get(a).points()).isZero(); assertThat(claims.count()).isEqualTo(1);
    }
    @Test void inactiveBenefitRejectedButRetryKeepsOriginalClaimAndCost() {
        String key=key(); long id=rewards.request(a,benefit,key);
        sql.update("update benefits set active=false, points_cost=30 where id=?",benefit);
        assertThatThrownBy(() -> rewards.request(b,benefit,key)).isInstanceOf(BusinessRuleException.class);
        assertThat(participants.get(b).points()).isEqualTo(20); assertThat(rewards.personal(a).getFirst().cost()).isEqualTo(20);
        assertThat(rewards.catalog()).isEmpty(); assertThat(rewards.request(a,benefit,key)).isEqualTo(id);
    }
    @Test void sameKeyCannotBeUsedForAnotherBenefit() {
        String key=key(); rewards.request(a,benefit,key);
        assertThatThrownBy(() -> rewards.request(a,benefit+999,key)).isInstanceOf(BusinessRuleException.class);
        assertThat(claims.count()).isEqualTo(1);
    }
    @Test void concurrentIdenticalRequestsHaveOneDebitAndOneId() throws Exception {
        String key=key(); Set<Long> ids=new HashSet<>();
        try(var pool=Executors.newFixedThreadPool(4)) {
            var start=new CountDownLatch(1); List<Future<Long>> jobs=new ArrayList<>();
            for(int i=0;i<4;i++) jobs.add(pool.submit(() -> { start.await(); return rewards.request(a,benefit,key); }));
            start.countDown(); for(var job:jobs) ids.add(job.get(15,TimeUnit.SECONDS));
        }
        assertThat(ids).hasSize(1); assertThat(claims.count()).isEqualTo(1); assertThat(participants.get(a).points()).isZero();
    }
    @Test void concurrentDifferentRequestsCannotOverdrawBalance() throws Exception {
        List<Boolean> results=new ArrayList<>();
        try(var pool=Executors.newFixedThreadPool(2)) {
            var start=new CountDownLatch(1); List<Future<Boolean>> jobs=new ArrayList<>();
            for(int i=0;i<2;i++) jobs.add(pool.submit(() -> { start.await(); try { rewards.request(a,benefit,key()); return true; } catch(BusinessRuleException e) { return false; } }));
            start.countDown(); for(var job:jobs) results.add(job.get(15,TimeUnit.SECONDS));
        }
        assertThat(results).containsExactlyInAnyOrder(true,false); assertThat(claims.count()).isEqualTo(1); assertThat(participants.get(a).points()).isZero();
    }
    @Test void pendingSagaCreditsCannotBeSpentAndCompensationPreservesPriorBalance() {
        long m=active(); matches.confirm(m,a); assertThat(steps.begin(m,b)).isTrue(); steps.grantFirst(m);
        assertThat(participants.get(a).points()).isEqualTo(30); assertThat(rewards.pendingCompletion(a)).isTrue();
        assertThatThrownBy(() -> rewards.request(a,benefit,key())).isInstanceOf(BusinessRuleException.class);
        assertThat(claims.count()).isZero(); steps.startCompensation(m); steps.reverseSecond(m); steps.reverseFirst(m); steps.restore(m);
        assertThat(participants.get(a).points()).isEqualTo(20); assertThat(rewards.pendingCompletion(a)).isFalse();
        rewards.request(a,benefit,key()); assertThat(participants.get(a).points()).isZero();
    }
    @Test void pendingSagaCanResumeThenParticipantSpendsOnlyCompletedRewards() {
        long m=active(); matches.confirm(m,a); steps.begin(m,b); steps.grantFirst(m);
        assertThatThrownBy(() -> rewards.request(b,benefit,key())).isInstanceOf(BusinessRuleException.class);
        saga.resume(m); rewards.request(a,benefit,key());
        assertThat(participants.get(a).points()).isEqualTo(10); assertThat(participants.get(b).points()).isEqualTo(30);
        assertThat(matches.get(m,a).status()).isEqualTo(MatchStatus.COMPLETED);
    }
    @Test void deliveryAndIncidentAreIdempotentWithoutAdditionalDebits() {
        long delivered=rewards.request(a,benefit,key()), incident=rewards.request(b,benefit,key());
        assertThatThrownBy(() -> rewards.finish(delivered,ClaimStatus.REQUESTED)).isInstanceOf(BusinessRuleException.class);
        rewards.finish(delivered,ClaimStatus.DELIVERED); rewards.finish(delivered,ClaimStatus.DELIVERED);
        rewards.finish(incident,ClaimStatus.INCIDENT); rewards.finish(incident,ClaimStatus.INCIDENT);
        assertThatThrownBy(() -> rewards.finish(delivered,ClaimStatus.INCIDENT)).isInstanceOf(BusinessRuleException.class);
        assertThat(participants.get(a).points()).isZero(); assertThat(participants.get(b).points()).isZero();
        var summary=rewards.forEvent(event); assertThat(summary.requested()).isZero(); assertThat(summary.delivered()).isEqualTo(1); assertThat(summary.incidents()).isEqualTo(1);
        assertThat(rewards.forEvent(event+999).claims()).isEmpty();
    }
    @Test void databaseRejectsDuplicateRequestKeys() {
        String key=key(); rewards.request(a,benefit,key);
        assertThatThrownBy(() -> sql.update("insert into reward_claims(participant_id,benefit_id,request_key,cost,status,created_at) values(?,?,?,20,'REQUESTED',CURRENT_TIMESTAMP)",a,benefit,key)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
    @Test void webUsesSelectedSessionParticipantAndServerCostAndShowsAdminResult() throws Exception {
        try(var browser=new TestBrowser(port)) {
            browser.get("/demo"); browser.post("/demo",Map.of("participantId",""+a));
            String body=browser.get("/me/benefits").body(); var key=Pattern.compile("name=\"requestKey\"[^>]*value=\"([^\"]+)\"").matcher(body); assertThat(key.find()).isTrue();
            Map<String,String> form=Map.of("benefitId",""+benefit,"requestKey",key.group(1),"participantId",""+b,"cost","1");
            assertThat(browser.post("/me/benefits",form).statusCode()).isEqualTo(302); browser.post("/me/benefits",form);
            assertThat(participants.get(a).points()).isZero(); assertThat(participants.get(b).points()).isEqualTo(20);
            long claim=rewards.personal(a).getFirst().id();
            assertThat(browser.get("/admin?eventId="+event).body()).contains("Snack del evento", "Marcar entregado", "Puntos emitidos");
            assertThat(browser.post("/admin/claims/"+claim,Map.of("status","DELIVERED")).statusCode()).isEqualTo(302);
            assertThat(browser.get("/me/benefits").body()).contains("Entregado");
            assertThat(browser.post("/admin/claims/"+claim,Map.of("status","INVALID")).statusCode()).isEqualTo(400);
        }
    }
    @Test void benefitWebRequiresSessionAndToken() throws Exception {
        try(var browser=new TestBrowser(port)) {
            assertThat(browser.get("/me/benefits").statusCode()).isEqualTo(401);
            assertThat(browser.post("/me/benefits",Map.of("benefitId",""+benefit,"requestKey",key())).statusCode()).isEqualTo(403);
            assertThat(claims.count()).isZero();
        }
    }
}
