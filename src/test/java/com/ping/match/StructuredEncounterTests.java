package com.ping.match;
import java.util.*;
import com.ping.encounter.*;
import com.ping.event.*;
import com.ping.participant.*;
import com.ping.web.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:structured;MODE=MySQL;DB_CLOSE_DELAY=-1") @ActiveProfiles("test")
class StructuredEncounterTests {
 @Autowired EventService events;@Autowired EncounterService config;@Autowired MatchService matches;@Autowired MatchingService matching;@Autowired ParticipantService people;@Autowired JdbcTemplate sql;
 long event,a,b,id,point,activity;
 @BeforeEach void setup(){sql.update("delete from feedback");sql.update("delete from match_reservations");sql.update("delete from ping_matches");sql.update("delete from participants");sql.update("delete from events");
  EventForm f=new EventForm();f.setName("Genérico");event=events.create(f).getId();events.transition(event,EventStatus.OPEN);
  point=point(event,"Patio","Junto al letrero público");ActivityForm act=new ActivityForm();act.setTitle("Una recomendación");act.setInstructions("Recomiéndense un libro.");act.setPointIds(Set.of(point));activity=config.saveActivity(event,null,act);
  a=person("Uno");b=person("Dos");id=matching.search(a);
 }
 long person(String name){ParticipantForm f=new ParticipantForm();f.setAlias(name);f.setEventId(event);f.setAvailable(true);return people.create(f).id();}
 long point(long e,String name,String directions){PointForm f=new PointForm();f.setName(name);f.setDirections(directions);return config.savePoint(e,null,f);}
 @Test void compatibleAssignmentFreezesAllInstructionsAndDisablingDoesNotAlterActive(){
  long incompatible=point(event,"Otro","No compatible");matches.accept(id,a);matches.accept(id,b);var before=matches.get(id,a);
  assertThat(before.meetingPoint()).isEqualTo("Patio");assertThat(before.meetingDirections()).isEqualTo("Junto al letrero público");assertThat(before.activityTitle()).isEqualTo("Una recomendación");
  var p=config.pointForm(event,point);p.setName("Cambiado");p.setActive(false);config.savePoint(event,point,p);
  var act=config.activityForm(event,activity);act.setInstructions("Nueva actividad");act.setActive(false);config.saveActivity(event,activity,act);
  assertThat(matches.get(id,b).activity()).isEqualTo(before.activity());assertThat(matches.get(id,b).meetingPoint()).isEqualTo("Patio");
  matches.confirm(id,a);matches.confirm(id,b);assertThat(people.get(a).points()).isEqualTo(10);
 }
 @Test void noCombinationKeepsProposalAndCanRecoverWithoutInventingFallback(){
  var p=config.pointForm(event,point);p.setActive(false);config.savePoint(event,point,p);matches.accept(id,a);
  assertThatThrownBy(()->matches.accept(id,b)).isInstanceOf(EncounterUnavailableException.class);
  assertThat(matches.get(id,a).status()).isEqualTo(MatchStatus.OFFERED);assertThat(matches.get(id,a).otherAccepted()).isFalse();assertThat(matches.get(id,a).word()).isNull();
  p=config.pointForm(event,point);p.setActive(true);config.savePoint(event,point,p);matches.accept(id,b);assertThat(matches.get(id,b).status()).isEqualTo(MatchStatus.ACTIVE);
 }
 @Test void interestConstrainedActivityIsNotAssignedToAlternativeWithoutCommonInterest(){
  var act=config.activityForm(event,activity);act.setInterestIds(Set.of(sql.queryForObject("select id from interests where name='Videojuegos'",Long.class)));config.saveActivity(event,activity,act);
  matches.accept(id,a);assertThatThrownBy(()->matches.accept(id,b)).isInstanceOf(EncounterUnavailableException.class);
  assertThat(config.assign(events.get(event),"Videojuegos",id).title()).isEqualTo("Una recomendación");
 }
 @Test void rejectsForeignPointsAndStaleEditsAndLegacyImportPreservesSources(){
  EventForm f=new EventForm();f.setName("Anterior");f.setMeetingPoints("Plaza\nEntrada");f.setEncounterActivities("Conversen sobre el evento.");long legacy=events.create(f).getId();
  config.importLegacy(legacy);config.importLegacy(legacy);assertThat(config.points(legacy)).hasSize(2);assertThat(config.activities(legacy)).hasSize(1);assertThat(events.get(legacy).getMeetingPoints()).isEqualTo("Plaza\nEntrada");
  var act=config.activityForm(event,activity);act.setPointIds(Set.of(config.points(legacy).getFirst().getId()));assertThatThrownBy(()->config.saveActivity(event,activity,act)).isInstanceOf(BusinessRuleException.class);
  var stale=config.pointForm(event,point);var edit=config.pointForm(event,point);edit.setDirections("Más indicaciones");config.savePoint(event,point,edit);assertThatThrownBy(()->config.savePoint(event,point,stale)).isInstanceOf(BusinessRuleException.class);
 }
}
