package com.ping.admin;
import java.util.*;
import com.ping.encounter.*;import com.ping.event.*;import com.ping.match.*;import com.ping.participant.*;import com.ping.reward.*;import com.ping.web.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.test.context.SpringBootTest;import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.test.context.ActiveProfiles;import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import static org.assertj.core.api.Assertions.*;import static org.mockito.Mockito.*;
/** «Puntos entregados» comes from the credit ledger, not from balances (DEC-049, anexo A). */
@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:delivered;MODE=MySQL;DB_CLOSE_DELAY=-1") @ActiveProfiles("test")
class DeliveredPointsMetricTests {
 @Autowired EventService events;@Autowired EncounterService config;@Autowired MatchService matches;@Autowired MatchingService matching;@Autowired ParticipantService people;
 @Autowired AdminService admin;@Autowired BenefitAdminService benefits;@Autowired RewardService rewards;@Autowired JdbcTemplate sql;
 @MockitoSpyBean CompletionRewardService credits;
 long event,other,activity;
 @BeforeEach void setup(){credits=org.springframework.test.util.AopTestUtils.getUltimateTargetObject(credits);reset(credits);
  EventForm e=new EventForm();e.setName("Puntos entregados");event=events.create(e).getId();events.transition(event,EventStatus.OPEN);
  EventForm o=new EventForm();o.setName("Otro evento");other=events.create(o).getId();
  PointForm p=new PointForm();p.setName("Punto público");p.setDirections("Mesa principal");long point=config.savePoint(event,null,p);
  ActivityForm f=new ActivityForm();f.setTitle("Conversar");f.setInstructions("Compartan una recomendación");f.setRewardPoints(15);f.setPointIds(Set.of(point));activity=config.saveActivity(event,null,f);
 }
 long person(String name){ParticipantForm p=new ParticipantForm();p.setAlias(name);p.setEventId(event);p.setAvailable(true);return people.create(p).id();}
 void amount(int n){var f=config.activityForm(event,activity);f.setRewardPoints(n);config.saveActivity(event,activity,f);}
 EventMetrics metrics(long id){return admin.metrics().stream().filter(m -> m.id()==id).findFirst().orElseThrow();}
 long balances(){return sql.queryForObject("select coalesce(sum(points),0) from participants where event_id=?",Long.class,event);}

 @Test void sumsNetCreditsOfDifferentAmountsAndIgnoresDuplicatesReversalsAndLaterClaims(){
  // Encounter 1 at +15 per person; repeated confirmations must not credit twice.
  long a=person("Ana"),b=person("Beto");long first=matching.search(a);
  matches.accept(first,a);matches.accept(first,b);matches.confirm(first,a);matches.confirm(first,b);matches.confirm(first,a);matches.confirm(first,b);
  assertThat(matches.get(first,a).status()).isEqualTo(MatchStatus.COMPLETED);
  assertThat(metrics(event).points()).isEqualTo(30);
  // Encounter 2 at +40 per person; its first attempt fails after crediting one person and is reversed.
  sql.update("update participants set available=false where id in (?,?)",a,b);amount(40);
  long c=person("Caro"),d=person("Dani");long second=matching.search(c);
  matches.accept(second,c);matches.accept(second,d);
  doThrow(new IllegalStateException("Test-only credit failure")).when(credits).grant(d,40);
  matches.confirm(second,c);assertThatThrownBy(()->matches.confirm(second,d)).isInstanceOf(BusinessRuleException.class);
  verify(credits).grant(c,40);verify(credits).reverse(c,40);
  assertThat(metrics(event).points()).as("granted then reversed counts as zero").isEqualTo(30);
  doCallRealMethod().when(credits).grant(d,40);matches.confirm(second,d);matches.confirm(second,d);
  assertThat(matches.get(second,c).status()).isEqualTo(MatchStatus.COMPLETED);
  var done=metrics(event);
  assertThat(done.completed()).isEqualTo(2);           // the old formula gave completed × 20 = 40
  assertThat(done.points()).isEqualTo(110);            // 2 × 15 + 2 × 40
  assertThat(balances()).isEqualTo(110);
  // A later claim spends balance but does not reduce points delivered.
  BenefitForm bf=new BenefitForm();bf.setName("Bebida");bf.setPointsCost(25);bf.setClaimLocation("Barra");bf.setClaimInstructions("Muestra tu canje");
  long benefit=benefits.save(event,null,bf);long claim=rewards.request(c,benefit,UUID.randomUUID().toString());rewards.finish(claim,ClaimStatus.DELIVERED);
  assertThat(people.get(c).points()).isEqualTo(15);assertThat(balances()).isEqualTo(85);
  assertThat(metrics(event).points()).isEqualTo(110);
  assertThat(metrics(other).points()).isZero();
 }
}
