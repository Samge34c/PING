package com.ping.match;

import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.TransactionDefinition;
import com.ping.event.*;
import com.ping.participant.*;
import com.ping.web.BusinessRuleException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:saga-tests;MODE=MySQL;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class MatchCompletionSagaTests {
    @Autowired MatchService matches;
    @Autowired MatchingService matching;
    @Autowired MatchRepository matchRepository;
    @Autowired MatchReservationRepository reservations;
    @Autowired FeedbackRepository feedback;
    @Autowired ParticipantService people;
    @Autowired ParticipantRepository participantRepository;
    @Autowired EventService events;
    @Autowired EventRepository eventRepository;
    @Autowired MatchCompletionSaga saga;
    @Autowired MatchCompletionRepository records;
    @Autowired CompletionStepRepository history;
    @Autowired JdbcTemplate sql;
    @Autowired PlatformTransactionManager transactions;
    @MockitoSpyBean CompletionRewardService rewards;
    @MockitoSpyBean MatchCompletionSagaSteps steps;
    MatchCompletionSagaSteps stepsSpy;
    long a, b, match;
    @BeforeEach void setup() {
        rewards = org.springframework.test.util.AopTestUtils.getUltimateTargetObject(rewards);
        stepsSpy = org.springframework.test.util.AopTestUtils.getUltimateTargetObject(steps);
        reset(rewards, stepsSpy);
        feedback.deleteAll(); reservations.deleteAll(); matchRepository.deleteAll(); participantRepository.deleteAll(); eventRepository.deleteAll();
        EventForm f = new EventForm(); f.setName("Demo Saga"); var event = events.create(f); events.transition(event.getId(),EventStatus.OPEN);
        ParticipantForm p = new ParticipantForm(); p.setEventId(event.getId()); p.setAvailable(true); p.setAlias("Samuel"); a = people.create(p).id();
        p.setAlias("Julián"); b = people.create(p).id(); match = matching.search(a); matches.accept(match,a); matches.accept(match,b); matches.arrive(match,a); matches.arrive(match,b);
    }
    MatchCompletion record() { return records.findById(match).orElseThrow(); }
    List<String> actions() { return history.findByMatchIdOrderByIdAsc(match).stream().map(CompletionStep::getAction).toList(); }
    void failSecond() { doThrow(new IllegalStateException("Test-only reward failure")).when(rewards).grant(b); }
    void confirmA() { matches.confirm(match,a); }
    void confirmB() { matches.confirm(match,b); }
    @Test void successfulSagaCommitsIdentifiableStepsAndCreditsBothOnce() {
        confirmA(); assertThat(records.count()).isZero(); assertThat(people.get(a).points()).isZero();
        confirmB(); assertThat(record().getStatus()).isEqualTo(SagaStatus.COMPLETED);
        assertThat(actions()).containsExactly("PREPARE_MATCH","GRANT_FIRST","GRANT_SECOND","CLOSE");
        assertThat(people.get(a).points()).isEqualTo(10); assertThat(people.get(b).points()).isEqualTo(10);
        assertThat(matches.get(match,a).status()).isEqualTo(MatchStatus.COMPLETED); assertThat(reservations.count()).isZero();
        confirmA(); confirmB(); saga.resume(match);
        assertThat(records.count()).isEqualTo(1); assertThat(history.count()).isEqualTo(4); assertThat(matchRepository.count()).isEqualTo(1);
        assertThat(people.get(a).points()).isEqualTo(10); assertThat(people.get(b).points()).isEqualTo(10);
    }
    @Test void failureAfterFirstCommittedCreditCompensatesWithoutErasingPreviousBalance() {
        sql.update("update participants set points=40 where id=?",a); sql.update("update participants set points=70 where id=?",b);
        doAnswer(call -> {
            TransactionTemplate independent = new TransactionTemplate(transactions);
            independent.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            long committed = independent.execute(s -> sql.queryForObject("select points from participants where id=?", Long.class,a));
            System.out.println("SAGA: crédito A confirmado en otra transacción; saldo=" + committed);
            assertThat(committed).isEqualTo(50); // Prueba que A se confirmó antes: no es rollback de una sola transacción.
            throw new IllegalStateException("Failure injected only in test before B");
        }).when(rewards).grant(b);
        confirmA(); assertThatThrownBy(this::confirmB).isInstanceOf(BusinessRuleException.class);
        assertThat(record().getStatus()).isEqualTo(SagaStatus.COMPENSATED);
        assertThat(people.get(a).points()).isEqualTo(40); assertThat(people.get(b).points()).isEqualTo(70);
        assertThat(actions()).containsExactly("PREPARE_MATCH","GRANT_FIRST","REVERSE_FIRST","RESTORE_MATCH");
        assertThat(matches.get(match,a).status()).isEqualTo(MatchStatus.ACTIVE);
        assertThat(matches.get(match,b).confirmed()).isFalse(); assertThat(matches.get(match,a).confirmed()).isTrue();
        assertThat(reservations.count()).isEqualTo(2);
        System.out.println("SAGA: " + record().getStatus() + "; A=" + people.get(a).points() + "; B=" + people.get(b).points() + "; pasos=" + actions());
    }
    @Test void failureInFirstRewardRestoresMatchWithNoReversalOfUnappliedCredit() {
        doThrow(new IllegalStateException("First reward failure")).when(rewards).grant(a);
        confirmA(); assertThatThrownBy(this::confirmB).isInstanceOf(BusinessRuleException.class);
        assertThat(actions()).containsExactly("PREPARE_MATCH","RESTORE_MATCH");
        assertThat(people.get(a).points()).isZero(); assertThat(people.get(b).points()).isZero();
        assertThat(record().getStatus()).isEqualTo(SagaStatus.COMPENSATED);
    }
    @Test void failureClosingCompensatesBothCreditsInReverseOrder() {
        doThrow(new IllegalStateException("Close failure in test")).when(stepsSpy).close(match);
        confirmA(); assertThatThrownBy(this::confirmB).isInstanceOf(BusinessRuleException.class);
        assertThat(actions()).containsExactly("PREPARE_MATCH","GRANT_FIRST","GRANT_SECOND","REVERSE_SECOND","REVERSE_FIRST","RESTORE_MATCH");
        assertThat(people.get(a).points()).isZero(); assertThat(people.get(b).points()).isZero();
        assertThat(matchRepository.findById(match).orElseThrow().isCompletionPending()).isFalse();
    }
    @Test void retryUsesSameSagaAndMatchWithNewAttemptAndNoDuplicateCredits() {
        failSecond(); confirmA(); assertThatThrownBy(this::confirmB).isInstanceOf(BusinessRuleException.class);
        doCallRealMethod().when(rewards).grant(b); confirmB(); confirmB(); confirmA();
        assertThat(record().getAttempt()).isEqualTo(2); assertThat(record().getStatus()).isEqualTo(SagaStatus.COMPLETED);
        assertThat(records.count()).isEqualTo(1); assertThat(matchRepository.count()).isEqualTo(1); assertThat(history.count()).isEqualTo(8);
        assertThat(people.get(a).points()).isEqualTo(10); assertThat(people.get(b).points()).isEqualTo(10);
    }
    @Test void repeatedCompensationDoesNotSubtractOrWriteTwice() {
        failSecond(); confirmA(); assertThatThrownBy(this::confirmB).isInstanceOf(BusinessRuleException.class);
        steps.startCompensation(match); steps.reverseSecond(match); steps.reverseFirst(match); steps.restore(match); saga.resume(match);
        assertThat(people.get(a).points()).isZero(); assertThat(history.count()).isEqualTo(4);
        assertThat(record().getStatus()).isEqualTo(SagaStatus.COMPENSATED);
    }
    @Test void recoveryResumesCommittedFirstStepUsingOnlyDatabaseState() {
        steps.begin(match,a); steps.begin(match,b); steps.grantFirst(match);
        assertThat(people.get(a).points()).isEqualTo(10); assertThat(people.get(b).points()).isZero();
        new MatchCompletionSaga(steps, records).recoverPending(); saga.recoverPending();
        assertThat(record().getStatus()).isEqualTo(SagaStatus.COMPLETED);
        assertThat(people.get(a).points()).isEqualTo(10); assertThat(people.get(b).points()).isEqualTo(10);
        assertThat(actions()).containsExactly("PREPARE_MATCH","GRANT_FIRST","GRANT_SECOND","CLOSE");
    }
    @Test void failedCompensationKeepsReservationAndRecoversWithoutNegativeBalance() {
        failSecond(); doThrow(new IllegalStateException("Reverse failure in test")).when(rewards).reverse(a);
        confirmA(); assertThatThrownBy(this::confirmB).isInstanceOf(BusinessRuleException.class);
        assertThat(record().getStatus()).isEqualTo(SagaStatus.FAILED); assertThat(record().isFirstGranted()).isTrue();
        assertThat(matches.get(match,a).confirmed()).isTrue(); assertThat(matches.get(match,b).completionPending()).isTrue();
        assertThat(reservations.count()).isEqualTo(2); assertThat(people.get(a).points()).isEqualTo(10);
        assertThatThrownBy(() -> matches.cancel(match,a)).isInstanceOf(BusinessRuleException.class);
        doCallRealMethod().when(rewards).reverse(a); saga.recoverPending(); saga.recoverPending();
        assertThat(record().getStatus()).isEqualTo(SagaStatus.COMPENSATED); assertThat(people.get(a).points()).isZero();
        assertThat(history.count()).isEqualTo(4); assertThat(reservations.count()).isEqualTo(2);
    }
    @Test void concurrentConfirmationsExecuteSingleCompletion() throws Exception {
        try(var pool = Executors.newFixedThreadPool(4)) {
            var start = new CountDownLatch(1); List<Future<?>> requests = new ArrayList<>();
            for(int i=0;i<4;i++){long id=i%2==0?a:b;requests.add(pool.submit(()->{start.await();matches.confirm(match,id);return null;}));}
            start.countDown(); for(var request:requests)request.get(15,TimeUnit.SECONDS);
        }
        assertThat(people.get(a).points()).isEqualTo(10); assertThat(people.get(b).points()).isEqualTo(10);
        assertThat(records.count()).isEqualTo(1); assertThat(history.count()).isEqualTo(4);
    }
    @Test void overflowIsARealFailureAndDoesNotLeavePartialCredits() {
        sql.update("update participants set points=? where id=?",Long.MAX_VALUE,b);
        confirmA(); assertThatThrownBy(this::confirmB).isInstanceOf(BusinessRuleException.class);
        assertThat(record().getStatus()).isEqualTo(SagaStatus.COMPENSATED);
        assertThat(people.get(a).points()).isZero(); assertThat(people.get(b).points()).isEqualTo(Long.MAX_VALUE);
    }
    @Test void recoveryAfterFinalCommitDoesNotReopenOrRepeatCredits() {
        steps.begin(match,a); steps.begin(match,b); steps.grantFirst(match); steps.grantSecond(match); steps.close(match);
        new MatchCompletionSaga(steps, records).recoverPending(); saga.resume(match);
        assertThat(record().getStatus()).isEqualTo(SagaStatus.COMPLETED); assertThat(history.count()).isEqualTo(4);
        assertThat(people.get(a).points()).isEqualTo(10); assertThat(people.get(b).points()).isEqualTo(10);
    }
}
