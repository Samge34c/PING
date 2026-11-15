package com.ping.verification;
import java.util.*;
import java.util.concurrent.*;
import com.ping.event.*;
import com.ping.encounter.*;
import com.ping.match.*;
import com.ping.participant.*;
import com.ping.web.BusinessRuleException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.*;

/**
 * DEC-073 · manual MySQL check (not part of the default suite: the class name does not end in "Tests").
 * Run only against a NEW exclusive database:
 *   PING_MYSQL_EXCLUSIVE_URL=jdbc:mysql://127.0.0.1:3321/<name>_exclusive?... mvnw -Dtest=SingleScanMySqlCheck test
 * Real row locks: simultaneous scans from both sides, and the same code posted twice at once, have exactly one effect.
 */
@SpringBootTest(properties={"spring.datasource.url=${PING_MYSQL_EXCLUSIVE_URL}","spring.datasource.username=${PING_MYSQL_EXCLUSIVE_USER:root}","spring.datasource.password=${PING_MYSQL_EXCLUSIVE_PASSWORD:}","spring.jpa.hibernate.ddl-auto=validate","ping.journey.encounter-policy=3"})
@ActiveProfiles("verification-test")
class SingleScanMySqlCheck {
 @Autowired EventService events;@Autowired EncounterService config;@Autowired ParticipantService people;@Autowired MatchingService matching;@Autowired MatchService matches;@Autowired CredentialService credentials;@Autowired JdbcTemplate sql;
 static final int ROUNDS=15;
 @BeforeAll static void exclusiveOnly(){String url=System.getenv("PING_MYSQL_EXCLUSIVE_URL");if(url==null||!url.startsWith("jdbc:mysql:")||!url.contains("exclusive"))throw new IllegalStateException("Use a NEW exclusive MySQL database only.");}
 long event;
 long person(String name){var f=new ParticipantForm();f.setAlias(name);f.setEventId(event);f.setAvailable(true);return people.create(f).id();}
 long[] arrivedPair(int round){var e=new EventForm();e.setName("Escaneo simultáneo ficticio "+round+" "+UUID.randomUUID());event=events.create(e).getId();events.transition(event,EventStatus.OPEN);var p=new PointForm();p.setName("Patio");p.setDirections("Letrero");long point=config.savePoint(event,null,p);var act=new ActivityForm();act.setTitle("Actividad ficticia");act.setInstructions("Hablen.");act.setRewardPoints(10);act.setPointIds(Set.of(point));config.saveActivity(event,null,act);
  long a=person("A"+round),b=person("B"+round);long m=matching.search(a);matches.accept(m,a);matches.accept(m,b);matches.arrive(m,a);matches.arrive(m,b);return new long[]{m,a,b};}
 int flags(long m){return sql.queryForObject("select (first_verified+0)+(second_verified+0) from ping_matches where id=?",Integer.class,m);}
 int consumed(long m){return sql.queryForObject("select count(*) from purpose_credentials where purpose='ENCOUNTER' and context_id=? and consumed=true",Integer.class,m);}
 List<Throwable> race(Callable<?> one,Callable<?> two)throws Exception{var gate=new CountDownLatch(1);var errors=new ArrayList<Throwable>();try(var pool=Executors.newFixedThreadPool(2)){var f1=pool.submit(()->{gate.await();return one.call();});var f2=pool.submit(()->{gate.await();return two.call();});gate.countDown();for(var f:List.of(f1,f2))try{f.get(30,TimeUnit.SECONDS);}catch(ExecutionException ex){errors.add(ex.getCause());}}return errors;}

 @Test void bothSidesScanningAtTheSameTimeHaveExactlyOneEffect()throws Exception{
  for(int round=0;round<ROUNDS;round++){long[] x=arrivedPair(round);long m=x[0],a=x[1],b=x[2];
   var ca=credentials.encounter(m,a,UUID.randomUUID().toString());var cb=credentials.encounter(m,b,UUID.randomUUID().toString());String ip="race-"+round;
   var errors=race(()->{credentials.verify(m,b,ca.manual(),ip);return null;},()->{credentials.verify(m,a,"PING:ENCOUNTER:"+cb.token(),ip);return null;});
   assertThat(errors).as("round %d: both calls answer without error",round).isEmpty();
   assertThat(flags(m)).as("round %d: exactly one real scan recorded",round).isEqualTo(1);
   assertThat(consumed(m)).as("round %d: exactly one code consumed",round).isEqualTo(1);
   assertThat(sql.queryForObject("select count(*) from purpose_credentials where purpose='ENCOUNTER' and context_id=? and consumed=false and revoked=false",Integer.class,m)).as("no usable code left").isZero();
   assertThat(matches.get(m,a).canConfirm()).isTrue();assertThat(matches.get(m,b).canConfirm()).isTrue();
   assertThat(people.get(a).points()+people.get(b).points()).as("verifying gives no points").isZero();}}

 @Test void theSameCodePostedTwiceAtOnceHasExactlyOneEffect()throws Exception{
  for(int round=0;round<ROUNDS;round++){long[] x=arrivedPair(100+round);long m=x[0],a=x[1],b=x[2];
   var ca=credentials.encounter(m,a,UUID.randomUUID().toString());String ip="double-"+round;
   var errors=race(()->{credentials.verify(m,b,ca.manual(),ip);return null;},()->{credentials.verify(m,b,ca.manual(),ip);return null;});
   assertThat(errors).as("round %d: a duplicate POST is answered as already verified",round).isEmpty();
   assertThat(flags(m)).isEqualTo(1);assertThat(consumed(m)).isEqualTo(1);
   assertThat(people.get(a).points()+people.get(b).points()).isZero();
   assertThatThrownBy(()->credentials.encounter(m,a,UUID.randomUUID().toString())).as("no new code after verification").isInstanceOf(BusinessRuleException.class);}}
}
