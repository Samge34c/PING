package com.ping.interest;

import com.ping.event.*;
import com.ping.participant.*;
import com.ping.support.TestBrowser;
import com.ping.web.BusinessRuleException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import java.util.*;
import java.util.regex.Pattern;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
 properties="spring.datasource.url=jdbc:h2:mem:category-admin;MODE=MySQL;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class InterestCategoryAdminTests {
 @Autowired InterestCategoryAdminService admin;
 @Autowired InterestService interests;
 @Autowired ParticipantService participants;
 @Autowired EventService events;
 @Autowired com.ping.encounter.EncounterService encounters;
 @Autowired com.ping.demo.DemoPreparationService demo;
 @Autowired JdbcTemplate sql;
 @LocalServerPort int port;
 long event;
 @BeforeEach void setup(){
  sql.update("update demo_scenarios set event_id=null");sql.update("delete from participant_interests");sql.update("delete from participants");sql.update("delete from events");
  sql.update("update interests set category_id=null");sql.update("delete from interest_categories where id>3");
  sql.update("update interest_categories set active=true,version=0");
  sql.update("update interests set category_id=1 where name in ('Videojuegos','Música','Cine','Anime')");
  sql.update("update interests set category_id=2 where name in ('Tecnología','Fotografía')");
  sql.update("update interests set category_id=3 where name in ('Deportes','Viajes')");
  EventForm f=new EventForm();f.setName("Categorías aisladas");event=events.create(f).getId();events.transition(event,EventStatus.OPEN);
 }
 long id(String name){return interests.catalog().stream().filter(i->i.getName().equals(name)).findFirst().orElseThrow().getId();}
 long person(){ParticipantForm f=new ParticipantForm();f.setAlias("Selección previa");f.setEventId(event);return participants.create(f).id();}
 Map<String,String> draft(String html){Map<String,String> f=new HashMap<>();for(String n:List.of("draftKey","revision")){var m=Pattern.compile("name=\""+n+"\"[^>]*value=\"([^\"]*)\"").matcher(html);assertThat(m.find()).isTrue();f.put(n,m.group(1));}return f;}
 @Test void createMoveAndUnassignPreserveParticipantLinksAndInvalidateOtherForms(){
  long person=person(),music=id("Música");var selection=interests.selection(person);selection.setInterestIds(List.of(music));interests.save(person,selection);
  var stale=admin.form(1);var f=new InterestCategoryForm();f.setName("Nueva agrupación");f.setPresentationOrder(5);f.setInterestIds(List.of(music));long category=admin.save(null,f);
  assertThat(category).isGreaterThan(3);assertThat(admin.list().getFirst().id()).isEqualTo(category);
  assertThat(admin.form(category).getInterestIds()).containsExactly(music);
  assertThat(admin.form(1).getInterestIds()).doesNotContain(music);
  assertThat(participants.get(person).interests()).containsExactly("Música");
  assertThatThrownBy(()->admin.save(1L,stale)).isInstanceOf(BusinessRuleException.class).hasMessageContaining("cambiaron");
  var empty=admin.form(category);empty.setInterestIds(List.of());admin.save(category,empty);
  assertThat(interests.groups(List.of())).anyMatch(g->g.name().equals("Otros intereses")&&g.choices().stream().anyMatch(c->c.id()==music));
  assertThat(participants.get(person).interests()).containsExactly("Música");
 }
 @Test void duplicateUnknownAndStaleAdminPostsDoNotWrite() throws Exception {
  try(var b=new TestBrowser(port)){
   assertThat(b.post("/interests/categories",Map.of("name","Sin CSRF")).statusCode()).isEqualTo(403);
   assertThat(b.get("/interests/categories/new").statusCode()).isEqualTo(200);
   var invalid=b.post("/interests/categories",Map.of("name","Categoría conservada","presentationOrder","-1","interestIds","1"));
   assertThat(invalid.statusCode()).isEqualTo(200);assertThat(invalid.body()).contains("conservada","checked=\"checked\"");
   assertThat(b.post("/interests/categories",Map.of("name","Cultura y entretenimiento","presentationOrder","1")).statusCode()).isEqualTo(409);
   assertThat(b.post("/interests/categories",Map.of("name","Inválida","presentationOrder","1","interestIds","999999")).statusCode()).isEqualTo(409);
   assertThat(admin.list()).hasSize(3);
   var f=admin.form(1);var stale=f.getVersion();f.setActive(false);admin.save(1L,f);
   b.get("/interests/categories/1/edit");
   assertThat(b.post("/interests/categories/1/edit",Map.of("name","Obsoleta","presentationOrder","10","version",""+stale,"active","true")).statusCode()).isEqualTo(409);
   assertThat(admin.form(1).isActive()).isFalse();
  }
 }
 @Test void editRetainsInactiveAndRejectsManipulatedNewChoiceWithoutLosingPostedActiveChoice() throws Exception {
  long person=person(),music=id("Música"),tech=id("Tecnología"),cine=id("Cine");var selection=interests.selection(person);selection.setInterestIds(List.of(music));interests.save(person,selection);
  var f=admin.form(1);f.setActive(false);admin.save(1L,f);
  try(var b=new TestBrowser(port)){
   String path="/participants/"+person+"/interests";var page=b.get(path);assertThat(page.body()).contains("Conservados","Música","No disponible para añadir");
   var response=b.postMulti(path,Map.of("version",List.of(""+participants.get(person).version()),"interestIds",List.of(""+music,""+tech,""+cine)));
   assertThat(response.statusCode()).isEqualTo(409);assertThat(response.body()).contains("checked=\"checked\"","Tecnología");
   assertThat(participants.get(person).interests()).containsExactly("Música");
   assertThat(b.postMulti(path,Map.of("version",List.of(""+participants.get(person).version()),"interestIds",List.of(""+music,""+tech))).statusCode()).isEqualTo(302);
   assertThat(participants.get(person).interests()).containsExactlyInAnyOrder("Música","Tecnología");
  }
 }
 @Test void deactivatedDraftCanGoBackRemoveChoiceFinishAndReplayOnlyOnce() throws Exception {
  try(var b=new TestBrowser(port)){
   b.get("/join?code="+events.get(event).getEntryCode());var details=draft(b.get("/join").body());details.put("alias","Borrador intacto");details.put("eventId",""+event);b.post("/join",details);
   var choices=draft(b.get("/join/interests").body());choices.put("interestIds",""+id("Música"));choices.put("action","back");b.post("/join/interests",choices);
   var f=admin.form(1);f.setActive(false);admin.save(1L,f);
   details=draft(b.get("/join").body());details.put("alias","Borrador intacto");details.put("eventId",""+event);b.post("/join",details);
   var page=b.get("/join/interests");assertThat(page.body()).contains("Conservados","Quítalas para terminar","checked=\"checked\"");choices=draft(page.body());choices.put("interestIds",""+id("Música"));
   assertThat(b.post("/join/interests",choices).statusCode()).isEqualTo(409);assertThat(sql.queryForObject("select count(*) from participants",Integer.class)).isZero();
   choices=draft(b.get("/join/interests").body());choices.put("interestIds",""+id("Tecnología"));
   assertThat(b.post("/join/interests",choices).statusCode()).isEqualTo(302);assertThat(b.post("/join/interests",choices).statusCode()).isEqualTo(302);
   assertThat(sql.queryForObject("select count(*) from participants",Integer.class)).isEqualTo(1);assertThat(b.get("/me").body()).contains("Borrador intacto","Tecnología");
  }
 }
 @Test void activityKeepsPreviousAssociationButCannotAddInactiveInterest(){
  var point=new com.ping.encounter.PointForm();point.setName("Patio público");point.setDirections("Junto al letrero de entrada");long place=encounters.savePoint(event,null,point);
  var activity=new com.ping.encounter.ActivityForm();activity.setTitle("Recomendar música");activity.setInstructions("Compartan una canción.");activity.setPointIds(Set.of(place));activity.setInterestIds(Set.of(id("Música")));long aid=encounters.saveActivity(event,null,activity);
  var category=admin.form(1);category.setActive(false);admin.save(1L,category);
  var kept=encounters.activityForm(event,aid);encounters.saveActivity(event,aid,kept);
  var invalid=encounters.activityForm(event,aid);invalid.setInterestIds(Set.of(id("Música"),id("Cine")));
  assertThatThrownBy(()->encounters.saveActivity(event,aid,invalid)).isInstanceOf(BusinessRuleException.class);
  assertThat(encounters.activityForm(event,aid).getInterestIds()).containsExactly(id("Música"));
 }
 @Test void preparingNewDemoSkipsUnavailablePresetsAndRemainsIdempotent(){
  var category=admin.form(1);category.setActive(false);admin.save(1L,category);
  var scenario=demo.prepare();assertThat(scenario.people()).hasSize(4);
  assertThat(scenario.people()).allSatisfy(p->assertThat(p.interests()).doesNotContain("Videojuegos","Anime","Música","Cine"));
  assertThat(scenario.people()).anySatisfy(p->assertThat(p.interests()).contains("Tecnología"));
  assertThat(demo.prepare().people()).isEqualTo(scenario.people());
 }
}
