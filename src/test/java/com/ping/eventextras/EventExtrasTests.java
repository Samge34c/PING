package com.ping.eventextras;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;
import com.ping.auth.*;
import com.ping.event.*;
import com.ping.encounter.*;
import com.ping.participant.*;
import com.ping.match.*;
import com.ping.support.*;
import com.ping.web.BusinessRuleException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"spring.datasource.url=jdbc:h2:mem:event-extras;MODE=MySQL;DB_CLOSE_DELAY=-1","spring.datasource.username=sa","spring.datasource.password=","spring.jpa.hibernate.ddl-auto=validate","ping.mail.from=extras@example.test","ping.public-base-url=http://127.0.0.1:8090"})
@ActiveProfiles("security-test")
class EventExtrasTests {
 @Autowired EventExperienceService extras;@Autowired ExperienceCard cards;@Autowired EventService events;@Autowired AccountService accounts;@Autowired ParticipantService people;@Autowired EncounterService encounters;@Autowired JdbcTemplate sql;@Autowired MatchingService matching;@Autowired RecognitionHintService hints;
 @MockitoBean JavaMailSender sender;@LocalServerPort int port;
 final List<MailCapture.Mail> mail=new CopyOnWriteArrayList<>();
 static final String PASSWORD="Fictitious event extras password 2026";
 long admin,owner,other,event,foreign,self,peer;String email,otherEmail;
 @BeforeEach void setup(){MailCapture.capture(sender,mail);String general="general-extras@example.test";if(accounts.find(general).isEmpty())accounts.bootstrap(general,PASSWORD);admin=accounts.find(general).orElseThrow();event=event("Experiencia real ");foreign=event("Otro evento ");email="owner-"+UUID.randomUUID()+"@example.test";otherEmail="other-"+UUID.randomUUID()+"@example.test";owner=account(email);other=account(otherEmail);self=person(event,"Propietario");peer=person(event,"PERSONA_AJENA_NO_EXPORTAR");accounts.bind(owner,self,event);accounts.bind(other,peer,event);as(admin);}
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 long event(String label){var f=new EventForm();f.setName(label+UUID.randomUUID());long id=events.create(f).getId();events.transition(id,EventStatus.OPEN);return id;}
 long account(String address){accounts.register(address,PASSWORD);var m=Pattern.compile("token=([A-Za-z0-9_-]{43})").matcher(mail.getLast().getText());assertThat(m.find()).isTrue();accounts.consume(m.group(1),"VERIFY",PASSWORD);return accounts.find(address).orElseThrow();}
 long person(long e,String alias){var f=new ParticipantForm();f.setEventId(e);f.setAlias(alias);f.setAvailable(true);return people.create(f).id();}
 void as(long id){var u=accounts.user(id);SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(u,null,u.getAuthorities()));}
 TestBrowser login(String address)throws Exception{var b=new TestBrowser(port);b.get("/login");assertThat(b.post("/login",Map.of("email",address,"password",PASSWORD)).statusCode()).isEqualTo(302);return b;}
 AgendaForm agenda(String title,String start){var f=new AgendaForm();f.setTitle(title);f.setStartsAt(start);return f;}
 @Test void oldEventsNeedNoNewConfigurationAndReadsDoNotCreateRows()throws Exception{
  assertThat(extras.settings(event).challengeEnabled()).isFalse();assertThat(extras.agenda(event)).isEmpty();
  assertThat(sql.queryForObject("select count(*) from event_experience_settings where event_id=?",Long.class,event)).isZero();
  assertThat(extras.preparation(event)).anySatisfy(r->assertThat(r.state()).contains("Sin plano"));
  try(var b=login(email)){for(String path:List.of("/me/help","/me/agenda","/me/challenges","/me/experience")){var r=b.get(path);assertThat(r.statusCode()).as(path).isEqualTo(200);assertThat(r.headers().firstValue("Cache-Control").orElse("")).contains("no-store");}}
 }
 @Test void configurationUsesFreshRevisionAndContactIsEscapedForParticipants()throws Exception{
  var f=extras.settingsForm(event);f.setStaffLocation("Mesa oficial del patio");f.setOfficialContact("<script>contacto</script>");extras.saveSettings(event,f);
  assertThatThrownBy(()->extras.saveSettings(event,f)).isInstanceOf(BusinessRuleException.class);
  assertThat(extras.settings(foreign).staffLocation()).isEmpty();
  try(var b=login(email)){assertThat(b.get("/me/help").body()).contains("Mesa oficial del patio","&lt;script&gt;contacto&lt;/script&gt;").doesNotContain("<script>contacto</script>");}
 }
 @Test void agendaIsOrderedScopedAndCreationRetriesAreIdempotent(){
  var late=agenda("Tarde","2026-11-05T16:00");long id=extras.saveAgenda(event,null,late);assertThat(extras.saveAgenda(event,null,late)).isEqualTo(id);
  extras.saveAgenda(event,null,agenda("Temprano","2026-11-05T10:00"));extras.saveAgenda(foreign,null,agenda("NO_VISIBLE_AJENO","2026-11-05T09:00"));
  assertThat(extras.agenda(event)).extracting(EventExperienceService.Agenda::title).containsExactly("Temprano","Tarde");
  var edit=extras.agendaForm(event,id);edit.setActive(false);extras.saveAgenda(event,id,edit);assertThat(extras.agenda(event)).hasSize(1);assertThat(extras.managedAgenda(event)).hasSize(2);
  assertThatThrownBy(()->extras.saveAgenda(event,id,edit)).isInstanceOf(BusinessRuleException.class);
 }
 @Test void agendaRejectsForeignPointsInvalidZonesAndAmbiguousTimes(){
  var point=new PointForm();point.setName("Punto ajeno");point.setDirections("Indicaciones");long p=encounters.savePoint(foreign,null,point);
  var f=agenda("Actividad","2026-11-05T10:00");f.setPointId(p);assertThatThrownBy(()->extras.saveAgenda(event,null,f)).isInstanceOf(BusinessRuleException.class);
  f.setPointId(null);f.setZoneId("No/SuchZone");assertThatThrownBy(()->extras.saveAgenda(event,null,f)).isInstanceOf(BusinessRuleException.class);
  f.setZoneId("America/New_York");f.setStartsAt("2026-03-08T02:30");assertThatThrownBy(()->extras.saveAgenda(event,null,f)).isInstanceOf(BusinessRuleException.class);
  f.setStartsAt("2026-11-01T01:30");assertThatThrownBy(()->extras.saveAgenda(event,null,f)).isInstanceOf(BusinessRuleException.class);
  assertThat(extras.agenda(event)).isEmpty();
 }
 @Test void agendaStoresInstantAndItsOwnZoneWhenTheDefaultChanges(){
  long id=extras.saveAgenda(event,null,agenda("Horario","2026-11-05T10:00"));var a=extras.agenda(event).getFirst();assertThat(a.startsAt()).isEqualTo(java.time.Instant.parse("2026-11-05T15:00:00Z"));
  var settings=extras.settingsForm(event);settings.setZoneId("Europe/Madrid");extras.saveSettings(event,settings);
  assertThat(extras.agendaForm(event,id).getStartsAt()).isEqualTo("2026-11-05T10:00");assertThat(extras.agendaForm(event,id).getZoneId()).isEqualTo("America/Bogota");
 }
 @Test void organizerAndParticipantRoutesEnforceEventScopeCsrfAndFreshRevocation()throws Exception{
  accounts.assign(admin,owner,event,true);
  try(var b=login(email);var anon=new TestBrowser(port)){
   assertThat(b.get("/events/"+event+"/preparation").statusCode()).isEqualTo(200);
   assertThat(b.get("/events/"+event+"/experience-settings").statusCode()).isEqualTo(200);
   assertThat(b.get("/events/"+event+"/agenda/new").statusCode()).isEqualTo(200);
   assertThat(b.get("/events/"+foreign+"/preparation").statusCode()).isEqualTo(403);
   assertThat(b.post("/events/"+foreign+"/experience-settings",Map.of("version","0","zoneId","America/Bogota")).statusCode()).isEqualTo(403);
   assertThat(anon.post("/events/"+event+"/agenda",Map.of("title","Intruso")).statusCode()).isEqualTo(403);
   accounts.assign(admin,owner,event,false);assertThat(b.get("/events/"+event+"/preparation").statusCode()).isEqualTo(403);
  }
  try(var b=login(otherEmail)){assertThat(b.get("/events/"+event+"/agenda").statusCode()).isEqualTo(403);assertThat(b.get("/me/agenda?eventId="+foreign).body()).doesNotContain("NO_VISIBLE_AJENO");}
 }
 @Test void privateCardUsesOwnRealCountsAndBalanceWithoutHintsCodesOrOtherIdentity()throws Exception{
  var h=hints.ownForm(self);h.setHint("PISTA_PRIVADA_NO_EXPORTAR");hints.save(self,h);
  long match=matching.search(self);sql.update("update ping_matches set status='COMPLETED',verification_policy=1 where id=?",match);sql.update("update participants set points=7 where id=?",self);
  var v=AvatarV3.defaults();sql.update("update participants set avatar_v3_animal='conejo',avatar_v3_palette='dorado',avatar_v3_head='bucket',avatar_v3_head_color='menta',avatar_v3_audio='none',avatar_v3_audio_color='violeta',avatar_v3_face='gafas-sol',avatar_v3_face_color='coral',avatar_v3_neck='panuelo',avatar_v3_neck_color='amarillo' where id=?",self);
  as(owner);var p=extras.personal(self);assertThat(p.completed()).isEqualTo(1);assertThat(p.balance()).isEqualTo(7);
  String svg=new String(cards.render(self),StandardCharsets.UTF_8);assertThat(svg).contains(events.get(event).getName(),"Saldo actual","Conexiones completadas","data:image/svg+xml;base64,").doesNotContain("PISTA_PRIVADA_NO_EXPORTAR","PERSONA_AJENA_NO_EXPORTAR",email,"purpose_credentials","/matches/","/me/claims/");
  assertThatThrownBy(()->extras.personal(peer)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
  try(var b=login(email)){var r=b.get("/me/experience/card.svg?download=true&participantId="+peer);assertThat(r.statusCode()).isEqualTo(200);assertThat(r.body()).doesNotContain("PERSONA_AJENA_NO_EXPORTAR");assertThat(r.headers().firstValue("Content-Disposition").orElse("")).contains("attachment","mi-experiencia-ping.svg");assertThat(r.headers().firstValue("Cache-Control").orElse("")).contains("no-store");}
 }
 @Test void optionalChallengeCountsHistoricalCompletionsOnceWithoutGrantingPoints(){
  long match=matching.search(self);sql.update("update ping_matches set status='COMPLETED',verification_policy=1 where id=?",match);
  sql.update("insert into ping_matches(event_id,first_id,second_id,status,first_wait,second_wait,common_interests,first_interests,second_interests,created_at) select event_id,first_id,second_id,'COMPLETED',first_wait,second_wait,common_interests,first_interests,second_interests,created_at from ping_matches where id=?",match);
  var f=extras.settingsForm(event);f.setChallengeEnabled(true);extras.saveSettings(event,f);as(owner);
  for(int i=0;i<3;i++){var p=extras.personal(self);assertThat(p.progress()).isEqualTo(2);assertThat(p.reached()).isTrue();assertThat(p.balance()).isZero();}
  assertThat(extras.settings(foreign).challengeEnabled()).isFalse();assertThat(sql.queryForObject("select verification_policy from ping_matches where id=?",Integer.class,match)).isEqualTo(1);
 }
 @Test void agendaStatesHaveRealLabelsAndNoInventedFinishForAnOpenEndedItem(){
  var f=agenda("Con horario","2026-11-05T10:00");f.setEndsAt("2026-11-05T11:00");extras.saveAgenda(event,null,f);var item=extras.agenda(event).getFirst();
  assertThat(item.state(java.time.Instant.parse("2026-11-05T14:59:00Z"))).isEqualTo("Próxima");
  assertThat(item.state(java.time.Instant.parse("2026-11-05T15:30:00Z"))).isEqualTo("Ahora");
  assertThat(item.state(java.time.Instant.parse("2026-11-05T16:00:00Z"))).isEqualTo("Terminada");
  extras.saveAgenda(foreign,null,agenda("Sin final","2026-11-05T10:00"));
  assertThat(extras.agenda(foreign).getFirst().state(java.time.Instant.parse("2026-11-06T00:00:00Z"))).isEqualTo("Iniciada · sin hora final");
 }
 @Test void cardWithZeroConnectionsIsARealDownloadWithoutInventedAchievements()throws Exception{
  as(owner);String svg=new String(cards.render(self),StandardCharsets.UTF_8);
  assertThat(extras.personal(self).completed()).isZero();
  assertThat(svg).contains("1080","1350","Propietario","Saldo actual").doesNotContain("Objetivo alcanzado","Explorador","PERSONA_AJENA_NO_EXPORTAR");
  var factory=javax.xml.parsers.DocumentBuilderFactory.newInstance();factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
  assertThat(factory.newDocumentBuilder().parse(new java.io.ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8))).getDocumentElement().getNodeName()).isEqualTo("svg");
 }
}
