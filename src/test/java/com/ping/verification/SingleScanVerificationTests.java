package com.ping.verification;
import java.util.*;
import java.util.regex.Pattern;
import com.ping.event.*;
import com.ping.encounter.*;
import com.ping.match.*;
import com.ping.participant.*;
import com.ping.auth.*;
import com.ping.web.*;
import com.ping.support.TestBrowser;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.assertj.core.api.Assertions.*;

/** DEC-073 · policy 3: one valid scan (QR or the partner's manual code) verifies the encounter for both, without points. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"spring.datasource.url=jdbc:h2:mem:single-scan;MODE=MySQL;DB_CLOSE_DELAY=-1","spring.datasource.username=sa","spring.datasource.password=","spring.jpa.hibernate.ddl-auto=validate","ping.public-base-url=http://127.0.0.1:8090","ping.journey.encounter-policy=3"})
@ActiveProfiles("verification-test")
class SingleScanVerificationTests {
 @Autowired EventService events;@Autowired EncounterService config;@Autowired ParticipantService people;@Autowired MatchingService matching;@Autowired MatchService matches;@Autowired CredentialService credentials;@Autowired AccountService accounts;@Autowired JdbcTemplate sql;@Autowired PasswordEncoder encoder;@Autowired MatchLiveState live;@LocalServerPort int port;
 long event,a,b,match,accountA,accountB;static final String PASSWORD="Contraseña exclusiva de fixture2026";
 @BeforeEach void setup(){var f=new EventForm();f.setName("Un escaneo ficticio "+UUID.randomUUID());event=events.create(f).getId();events.transition(event,EventStatus.OPEN);var p=new PointForm();p.setName("Patio público");p.setDirections("Junto al letrero");long point=config.savePoint(event,null,p);var activity=new ActivityForm();activity.setTitle("Una recomendación");activity.setInstructions("Compartan una recomendación.");activity.setRewardPoints(37);activity.setPointIds(Set.of(point));config.saveActivity(event,null,activity);accountA=account();accountB=account();a=person(accountA,"Persona A");b=person(accountB,"Persona B");match=matching.search(a);matches.accept(match,a);matches.accept(match,b);}
 long account(){String email=UUID.randomUUID()+"@fixture.test";sql.update("insert into ping_accounts(email,password_hash,email_verified) values(?,?,true)",email,encoder.encode(PASSWORD));return accounts.find(email).orElseThrow();}
 long person(long account,String name){var f=new ParticipantForm();f.setAlias(name);f.setEventId(event);f.setAvailable(true);long id=people.create(f).id();accounts.bind(account,id,event);return id;}
 String key(){return UUID.randomUUID().toString();}
 String ip(){return "fixture-"+event;}
 void arrived(){matches.arrive(match,a);matches.arrive(match,b);}
 CredentialService.Issued issue(long self){return credentials.encounter(match,self,key());}
 int consumed(){return sql.queryForObject("select count(*) from purpose_credentials where purpose='ENCOUNTER' and context_id=? and consumed=true",Integer.class,match);}
 void login(TestBrowser browser,long account)throws Exception{browser.get("/login");assertThat(browser.post("/login",Map.of("email",accounts.user(account).email(),"password",PASSWORD)).statusCode()).isEqualTo(302);}
 void unlockedForBoth(){for(long self:List.of(a,b)){assertThat(matches.get(match,self).canConfirm()).isTrue();assertThat(live.forParticipant(self).get("step")).isEqualTo("activity");}}

 @Test void newEncountersFreezePolicyThreeAndStillRequireBothArrivals(){
  assertThat(matches.get(match,a).verificationPolicy()).isEqualTo(3);
  assertThatThrownBy(()->issue(a)).as("no code before both arrivals").isInstanceOf(BusinessRuleException.class);
  matches.arrive(match,a);assertThatThrownBy(()->issue(a)).isInstanceOf(BusinessRuleException.class);
  assertThatThrownBy(()->matches.confirm(match,a)).isInstanceOf(BusinessRuleException.class);
  matches.arrive(match,b);assertThatThrownBy(()->matches.confirm(match,a)).as("arrivals alone do not verify").isInstanceOf(BusinessRuleException.class);
  assertThat(live.forParticipant(a).get("step")).isEqualTo("verify");}

 @Test void oneQrScanOpensTheActivityForBothWithoutPoints(){
  arrived();var ca=issue(a);credentials.verify(match,b,"PING:ENCOUNTER:"+ca.token(),ip());
  unlockedForBoth();
  assertThat(matches.get(match,b).verified()).as("B really scanned").isTrue();assertThat(matches.get(match,a).verified()).as("A is not marked as having scanned").isFalse();
  assertThat(people.get(a).points()).isZero();assertThat(people.get(b).points()).isZero();
  matches.confirm(match,a);assertThat(people.get(a).points()).isZero();matches.confirm(match,b);matches.confirm(match,b);
  assertThat(people.get(a).points()).isEqualTo(37);assertThat(people.get(b).points()).isEqualTo(37);}

 @Test void theManualCodeWorksFromTheOtherSideToo(){
  arrived();var cb=issue(b);credentials.verify(match,a,cb.manual(),ip());unlockedForBoth();
  assertThat(people.get(a).points()).isZero();assertThat(people.get(b).points()).isZero();}

 @Test void invalidCodesAreRejectedAndVerifyNothing(){
  arrived();var ca=issue(a);
  assertThatThrownBy(()->credentials.verify(match,a,ca.manual(),ip())).as("own code").isInstanceOf(BusinessRuleException.class);
  assertThatThrownBy(()->credentials.verify(match,b,matches.get(match,b).word(),ip())).as("encounter word").isInstanceOf(BusinessRuleException.class);
  assertThatThrownBy(()->credentials.verify(match,b,"PING:REDEMPTION:"+ca.token(),ip())).as("wrong purpose").isInstanceOf(BusinessRuleException.class);
  assertThatThrownBy(()->credentials.verify(match,b,"INVALIDO",ip())).as("garbage").isInstanceOf(BusinessRuleException.class);
  long c=person(account(),"Persona C"),d=person(account(),"Persona D");long foreign=matching.search(c);matches.accept(foreign,c);matches.accept(foreign,d);matches.arrive(foreign,c);matches.arrive(foreign,d);
  var cc=credentials.encounter(foreign,c,key());
  assertThatThrownBy(()->credentials.verify(match,b,cc.manual(),ip())).as("code of another encounter").isInstanceOf(BusinessRuleException.class);
  assertThatThrownBy(()->credentials.verify(match,c,ca.manual(),ip())).as("third party").isInstanceOf(BusinessRuleException.class);
  sql.update("update purpose_credentials set expires_at=? where id=?",java.sql.Timestamp.from(java.time.Instant.now().minusSeconds(60)),ca.id());
  assertThatThrownBy(()->credentials.verify(match,b,ca.manual(),ip())).as("expired").isInstanceOf(BusinessRuleException.class);
  assertThat(matches.get(match,a).canConfirm()).isFalse();assertThat(matches.get(match,a).verified()).isFalse();assertThat(matches.get(match,b).verified()).isFalse();}

 @Test void repeatedAndSecondCodesHaveNoFurtherEffect(){
  arrived();var ca=issue(a);var cb=issue(b);
  credentials.verify(match,b,ca.manual(),ip());
  credentials.verify(match,b,ca.manual(),ip());
  credentials.verify(match,b,"PING:ENCOUNTER:"+ca.token(),ip());
  credentials.verify(match,a,cb.manual(),ip());
  assertThat(matches.get(match,b).verified()).isTrue();assertThat(matches.get(match,a).verified()).as("the second code did not add a scan").isFalse();
  assertThat(consumed()).as("exactly one effective scan").isEqualTo(1);
  assertThat(sql.queryForObject("select revoked from purpose_credentials where id=?",Boolean.class,cb.id())).as("the other pending code is revoked").isTrue();
  assertThatThrownBy(()->issue(a)).as("no new codes once verified").isInstanceOf(BusinessRuleException.class);
  assertThatThrownBy(()->issue(b)).isInstanceOf(BusinessRuleException.class);
  unlockedForBoth();assertThat(people.get(a).points()).isZero();assertThat(people.get(b).points()).isZero();}

 @Test void cardAsksForOneScanAndBothSessionsMoveToActivityOverHttp()throws Exception{
  try(var aa=new TestBrowser(port);var bb=new TestBrowser(port)){login(aa,accountA);login(bb,accountB);String path="/matches/"+match;
   aa.get(path);bb.get(path);aa.post(path+"/arrive",Map.of());bb.post(path+"/arrive",Map.of());
   for(var browser:List.of(aa,bb)){String html=browser.get(path).body();
    assertThat(html).contains("data-phase=\"verify\"","data-sub=\"one\"","Verifiquen el encuentro","Uno muestra su QR y el otro lo escanea","Escanear su QR","Mostrar mi QR","Escribir su código")
      .doesNotContain("Verificaste a","aún no te verifica","Una recomendación","data-private-credential");}
   aa.post(path+"/code",Map.of("requestKey",key()));
   var m=Pattern.compile("<code[^>]*data-private-credential[^>]*>([^<]+)</code>").matcher(aa.get(path+"?view=mine").body());assertThat(m.find()).isTrue();
   assertThat(bb.post(path+"/verify",Map.of("credential",m.group(1))).statusCode()).isEqualTo(302);
   assertThat(bb.post(path+"/verify",Map.of("credential",m.group(1))).statusCode()).as("a repeated POST is harmless").isEqualTo(302);
   for(var browser:List.of(aa,bb)){String html=browser.get(path).body();
    assertThat(html).contains("data-phase=\"activity\"","Una recomendación","id=\"am-confirm-action\"").doesNotContain("data-private-credential","Escanear su QR");}
   assertThat(aa.get(path+"?view=mine").body()).as("no QR once the phase is done").doesNotContain("data-private-credential");
   assertThat(people.get(a).points()).isZero();}}
}
