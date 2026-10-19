package com.ping.event;
import com.ping.encounter.*;
import com.ping.participant.*;
import com.ping.interest.*;
import com.ping.reward.*;
import com.ping.web.BusinessRuleException;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:work-editor;MODE=MySQL;DB_CLOSE_DELAY=-1") @ActiveProfiles("test")
class WorkEditorTests{
 @Autowired EventService events;@Autowired EncounterService encounters;@Autowired ParticipantService people;@Autowired InterestTaxonomy taxonomy;@Autowired JdbcTemplate sql;@Autowired BenefitAdminService benefits;@Autowired RewardService rewards;
 long event;
 @BeforeEach void init(){var f=new EventForm();f.setName("Evento ficticio Work");f.setDeliveryName("Mesa de beneficios");f.setDeliveryDirections("En el vestíbulo público");event=events.create(f).getId();events.transition(event,EventStatus.OPEN);}
 long point(String name){var f=new PointForm();f.setName(name);f.setDirections("Letrero visible");return encounters.savePoint(event,null,f);}
 @Test void enforcesPointLimitEvenInactiveAndAllowsExplicitArchive(){long p=point("Uno");point("Dos");point("Tres");assertThatThrownBy(()->point("Cuatro")).isInstanceOf(BusinessRuleException.class);var f=encounters.pointForm(event,p);f.setActive(false);encounters.savePoint(event,p,f);assertThatThrownBy(()->point("Cuatro")).isInstanceOf(BusinessRuleException.class);encounters.archivePoint(event,p);point("Cuatro");assertThat(encounters.points(event)).hasSize(4);}
 @Test void simultaneousCreatesCannotExceedLimit()throws Exception{point("Uno");point("Dos");try(var executor=Executors.newFixedThreadPool(2)){var gate=new CountDownLatch(1);var tasks=new ArrayList<Future<Boolean>>();for(int i=0;i<2;i++)tasks.add(executor.submit(()->{gate.await();try{point("Concurrente");return true;}catch(BusinessRuleException e){return false;}}));gate.countDown();assertThat(tasks.get(0).get()^tasks.get(1).get()).isTrue();}assertThat(encounters.points(event)).hasSize(3);}
 @Test void activitiesAndWordsAreBoundedAndWordsFoldAccents(){long p=point("Uno");for(int i=0;i<10;i++){var f=new ActivityForm();f.setTitle("Actividad"+i);f.setInstructions("Conversen");f.setPointIds(Set.of(p));encounters.saveActivity(event,null,f);}var f=new ActivityForm();f.setTitle("Exceso");f.setInstructions("Conversen");f.setPointIds(Set.of(p));assertThatThrownBy(()->encounters.saveActivity(event,null,f)).isInstanceOf(BusinessRuleException.class);encounters.words(event," Café , CAFE,   Hola   mundo ",events.get(event).getVersion());assertThat(EncounterOptions.lines(events.get(event).getEncounterWords())).containsExactly("Café","Hola mundo");var words=java.util.stream.IntStream.range(0,101).mapToObj(i->"PALABRA"+i).collect(java.util.stream.Collectors.joining("\n"));assertThatThrownBy(()->encounters.words(event,words,events.get(event).getVersion())).isInstanceOf(BusinessRuleException.class);}
 @Test void legacyExcessIsPreservedAndCannotGrowOrPartiallyImport(){sql.update("update events set meeting_points='Uno\nDos\nTres\nCuatro' where id=?",event);var f=EventForm.from(events.get(event));f.setName("Editado");events.update(event,f);assertThatThrownBy(()->encounters.importLegacy(event)).isInstanceOf(BusinessRuleException.class);assertThat(encounters.points(event)).isEmpty();f=EventForm.from(events.get(event));f.setMeetingPoints(f.getMeetingPoints()+"\nCinco");var changed=f;assertThatThrownBy(()->events.update(event,changed)).isInstanceOf(BusinessRuleException.class);}
 @Test void catalogueKeepsHistoricalIdsAndSingleMembershipIdentity(){assertThat(sql.queryForObject("select count(*) from interests where demo=true and id between 1 and 8",Integer.class)).isEqualTo(8);long lady=sql.queryForObject("select id from interests where stable_key='interest:lady-gaga'",Long.class);assertThat(taxonomy.memberships().get(lady)).contains("music:pop");assertThat(sql.queryForObject("select count(distinct stable_key) from interests",Integer.class)).isEqualTo(264);}
 @Test void avatarAllowlistAndDefaultDeliverySnapshotRemainStable(){assertThatThrownBy(()->new Avatar("../gato","none","none","violeta")).isInstanceOf(BusinessRuleException.class);var p=new ParticipantForm();p.setAlias("Ficticio");p.setEventId(event);long id=people.create(p).id();people.avatar(id,new Avatar("rana","gorra","gafas","menta"));assertThat(people.avatar(id).animal()).isEqualTo("rana");sql.update("update participants set points=50 where id=?",id);var f=new BenefitForm();f.setName("Beneficio ficticio");f.setDefaultDelivery(true);long b=benefits.save(event,null,f);String key=UUID.randomUUID().toString();long claim=rewards.request(id,b,key);assertThat(rewards.request(id,b,key)).isEqualTo(claim);var ef=EventForm.from(events.get(event));ef.setDeliveryName("Nuevo lugar");events.update(event,ef);assertThat(rewards.personal(id).getFirst().claimLocation()).isEqualTo("Mesa de beneficios");assertThat(people.get(id).points()).isEqualTo(30);}
}
