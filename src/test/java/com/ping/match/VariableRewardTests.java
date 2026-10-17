package com.ping.match;
import java.util.*;
import com.ping.encounter.*;import com.ping.event.*;import com.ping.participant.*;import com.ping.reward.*;import com.ping.web.*;
import org.junit.jupiter.api.*;import org.junit.jupiter.params.ParameterizedTest;import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.test.context.SpringBootTest;import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.test.context.ActiveProfiles;import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import static org.assertj.core.api.Assertions.*;import static org.mockito.Mockito.*;
@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:variable;MODE=MySQL;DB_CLOSE_DELAY=-1") @ActiveProfiles("test")
class VariableRewardTests {
 @Autowired EventService events;@Autowired EncounterService config;@Autowired MatchService matches;@Autowired MatchingService matching;@Autowired ParticipantService people;@Autowired JdbcTemplate sql;
 @Autowired MatchCompletionSaga saga;@Autowired MatchCompletionRepository records;@Autowired BenefitAdminService admin;@Autowired RewardService rewards;
 @MockitoSpyBean CompletionRewardService credits;@MockitoSpyBean MatchCompletionSagaSteps steps;
 MatchCompletionSagaSteps stepsSpy;long event,a,b,id,activity;
 @BeforeEach void setup(){credits=org.springframework.test.util.AopTestUtils.getUltimateTargetObject(credits);stepsSpy=org.springframework.test.util.AopTestUtils.getUltimateTargetObject(steps);reset(credits,stepsSpy);
  sql.update("delete from feedback");sql.update("delete from match_reservations");sql.update("delete from ping_matches");sql.update("delete from participants");sql.update("delete from events");
  EventForm e=new EventForm();e.setName("Puntos variables");event=events.create(e).getId();events.transition(event,EventStatus.OPEN);
  PointForm p=new PointForm();p.setName("Punto público");p.setDirections("Mesa principal");long point=config.savePoint(event,null,p);
  ActivityForm f=new ActivityForm();f.setTitle("Conversar");f.setInstructions("Compartan una recomendación");f.setRewardPoints(37);f.setPointIds(Set.of(point));activity=config.saveActivity(event,null,f);
  a=person("Uno");b=person("Dos");id=matching.search(a);
 }
 long person(String name){ParticipantForm p=new ParticipantForm();p.setAlias(name);p.setEventId(event);p.setAvailable(true);return people.create(p).id();}
 void activate(){matches.accept(id,a);matches.accept(id,b);}
 void amount(int n){var f=config.activityForm(event,activity);f.setRewardPoints(n);config.saveActivity(event,activity,f);}
 @ParameterizedTest @ValueSource(ints={0,37,100}) void amountIsFrozenAndGrantedExactlyOnceWithoutArrivals(int value){amount(value);activate();amount(value==100?0:100);assertThat(matches.get(id,a).rewardPoints()).isEqualTo(value);matches.confirm(id,a);assertThat(people.get(a).points()).isZero();matches.confirm(id,b);matches.confirm(id,a);matches.confirm(id,b);assertThat(people.get(a).points()).isEqualTo(value);assertThat(people.get(b).points()).isEqualTo(value);assertThat(records.findById(id).orElseThrow().getRewardPoints()).isEqualTo(value);assertThat(matches.get(id,a).status()).isEqualTo(MatchStatus.COMPLETED);}
 @Test void secondCreditFailureReversesExactAmountAndRetryKeepsOriginalSnapshot(){activate();sql.update("update participants set points=20 where id in (?,?)",a,b);doThrow(new IllegalStateException("Test only")).when(credits).grant(b,37);
  matches.confirm(id,a);assertThatThrownBy(()->matches.confirm(id,b)).isInstanceOf(BusinessRuleException.class);assertThat(people.get(a).points()).isEqualTo(20);assertThat(people.get(b).points()).isEqualTo(20);assertThat(records.findById(id).orElseThrow().getStatus()).isEqualTo(SagaStatus.COMPENSATED);
  amount(100);doCallRealMethod().when(credits).grant(b,37);matches.confirm(id,b);matches.confirm(id,b);assertThat(people.get(a).points()).isEqualTo(57);assertThat(people.get(b).points()).isEqualTo(57);
 }
 @Test void closingFailureReversesBothVariableCredits(){activate();doThrow(new IllegalStateException("Test close")).when(stepsSpy).close(id);matches.confirm(id,a);assertThatThrownBy(()->matches.confirm(id,b)).isInstanceOf(BusinessRuleException.class);assertThat(people.get(a).points()).isZero();assertThat(people.get(b).points()).isZero();verify(credits).reverse(b,37);verify(credits).reverse(a,37);}
 @Test void failedCompensationIsDurableBlocksSpendingAndRecoversIdempotently(){activate();doThrow(new IllegalStateException("Test credit")).when(credits).grant(b,37);doThrow(new IllegalStateException("Test reverse")).when(credits).reverse(a,37);
  matches.confirm(id,a);assertThatThrownBy(()->matches.confirm(id,b)).isInstanceOf(BusinessRuleException.class);assertThat(records.findById(id).orElseThrow().getStatus()).isEqualTo(SagaStatus.FAILED);assertThat(people.get(a).points()).isEqualTo(37);
  long benefit=rewards.catalog().getFirst().getId();assertThatThrownBy(()->rewards.request(a,benefit,UUID.randomUUID().toString())).isInstanceOf(BusinessRuleException.class);
  doCallRealMethod().when(credits).reverse(a,37);saga.recoverPending();saga.recoverPending();assertThat(people.get(a).points()).isZero();assertThat(matches.get(id,a).completionPending()).isFalse();
 }
 @Test void recoveryUsesDurableReceiptAndDoesNotCreditFirstTwice(){activate();steps.begin(id,a);steps.begin(id,b);steps.grantFirst(id);assertThat(people.get(a).points()).isEqualTo(37);amount(100);saga.recoverPending();saga.recoverPending();assertThat(people.get(a).points()).isEqualTo(37);assertThat(people.get(b).points()).isEqualTo(37);}
 @Test void valuesOutsideBoundsAreRejectedAndDoNotChangeConfiguredAmount(){assertThatThrownBy(()->amount(-1)).isInstanceOf(jakarta.validation.ConstraintViolationException.class);assertThatThrownBy(()->amount(101)).isInstanceOf(jakarta.validation.ConstraintViolationException.class);assertThat(config.activityForm(event,activity).getRewardPoints()).isEqualTo(37);}
}
