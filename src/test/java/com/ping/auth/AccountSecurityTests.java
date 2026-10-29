package com.ping.auth;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;
import com.ping.event.*;
import com.ping.participant.*;
import com.ping.support.TestBrowser;
import com.ping.web.BusinessRuleException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"spring.datasource.url=jdbc:h2:mem:account-security;MODE=MySQL;DB_CLOSE_DELAY=-1","spring.datasource.username=sa","spring.datasource.password=","spring.jpa.hibernate.ddl-auto=validate","ping.mail.from=ping@example.test","ping.public-base-url=http://127.0.0.1:8090"}) @ActiveProfiles("security-test")
class AccountSecurityTests {
 @org.springframework.test.context.bean.override.mockito.MockitoSpyBean AccountService accounts;@Autowired EventService events;@Autowired ParticipantService participants;@Autowired JdbcTemplate jdbc;@Autowired AuthThrottle throttle;@Autowired com.ping.interest.InterestService interests;@LocalServerPort int port;@MockitoBean JavaMailSender sender;
 final List<com.ping.support.MailCapture.Mail> mail=new CopyOnWriteArrayList<>();static final String PASSWORD="Prueba local de cuenta2026";
 @BeforeEach void transport(){mail.clear();com.ping.support.MailCapture.capture(sender,mail);}
 String address(){return "persona-"+UUID.randomUUID()+"@example.test";}
 String token(){var m=Pattern.compile("token=([A-Za-z0-9_-]{43})").matcher(mail.getLast().getText());assertThat(m.find()).isTrue();return m.group(1);}
 long account(String email){accounts.register(email,PASSWORD);accounts.consume(token(),"VERIFY",PASSWORD);return accounts.find(email).orElseThrow();}
 long event(String name){var f=new EventForm();f.setName(name+UUID.randomUUID());long id=events.create(f).getId();events.transition(id,EventStatus.OPEN);return id;}
 void login(TestBrowser b,String email)throws Exception{b.get("/login");assertThat(b.post("/login",Map.of("email",email,"password",PASSWORD)).statusCode()).isEqualTo(302);}
 long general(){String email="general-setup@example.test";if(accounts.find(email).isEmpty())accounts.bootstrap(email,PASSWORD);return accounts.find(email).orElseThrow();}
 @Test void anonymousCannotReadOrMutateAdministrativeRoutes()throws Exception{try(var b=new TestBrowser(port)){for(String p:List.of("/events","/participants","/admin","/organizer","/interests/categories"))assertThat(b.get(p).statusCode()).isEqualTo(302);assertThat(b.post("/events",Map.of("name","Intruso")).statusCode()).isEqualTo(403);assertThat(b.get("/me/state").statusCode()).isEqualTo(401);}}
 @Test void registrationVerificationAndReplayRequireCsrfAndPersistOnlyTokenHashes()throws Exception{String email=address();try(var b=new TestBrowser(port)){b.get("/register");var response=b.post("/register",Map.of("email",email,"password",PASSWORD,"repeat",PASSWORD,"role","ADMIN"));assertThat(response.headers().firstValue("Location").orElse("")).endsWith("/register/check");long id=accounts.find(email).orElseThrow();assertThat(accounts.user(id).general()).isFalse();assertThat(accounts.user(id).verified()).isFalse();String t=token();assertThat(jdbc.queryForObject("select token_hash from account_tokens where account_id=?",String.class,id)).isNotEqualTo(t).hasSize(64);assertThat(b.post("/login",Map.of("email",email,"password",PASSWORD)).headers().firstValue("Location").orElse("")).contains("error");try(var missing=new TestBrowser(port)){assertThat(missing.post("/account/verify",Map.of("token",t,"password",PASSWORD)).statusCode()).isEqualTo(403);}b.get("/account/verify?token="+t);assertThat(b.post("/account/verify",Map.of("token",t,"password",PASSWORD)).statusCode()).isEqualTo(302);assertThat(accounts.user(id).verified()).isTrue();b.get("/account/verify?token="+t);assertThat(b.post("/account/verify",Map.of("token",t,"password",PASSWORD)).body()).contains("ha caducado");}}
 @Test void participantCannotEscalateRolesOrAdoptAnotherIdentity()throws Exception{String email=address();long account=account(email);long e=event("Privado");var f=new ParticipantForm();f.setAlias("Mi alias persistente");f.setEventId(e);long own=participants.create(f).id();accounts.bind(account,own,e);f.setAlias("Alias ajeno");long other=participants.create(f).id();try(var b=new TestBrowser(port)){login(b,email);assertThat(b.get("/me").body()).contains("Mi alias persistente").doesNotContain("Alias ajeno");assertThat(b.get("/participants/"+other+"/interests").statusCode()).isEqualTo(403);assertThat(b.get("/events/"+e+"/edit").statusCode()).isEqualTo(403);assertThat(b.get("/%65vents/"+e+"/edit").statusCode()).isEqualTo(403);assertThat(b.post("/organizer/access",Map.of("email",email,"eventId",""+e,"grant","true")).statusCode()).isEqualTo(403);assertThat(b.post("/demo",Map.of("participantId",""+other)).statusCode()).isEqualTo(403);}}
 @Test void eventAdministratorIsLimitedToAssignedEventEvenForDirectPost()throws Exception{long general=general();String email=address();long id=account(email),allowed=event("Asignado"),foreign=event("AJENO");accounts.assign(general,id,allowed,true);try(var b=new TestBrowser(port)){login(b,email);assertThat(b.get("/events/"+allowed+"/edit").statusCode()).isEqualTo(200);assertThat(b.get("/events/"+foreign+"/edit").statusCode()).isEqualTo(403);assertThat(b.get("/admin?eventId="+foreign).statusCode()).isEqualTo(403);assertThat(b.get("/events").body()).doesNotContain(events.get(foreign).getName());assertThat(b.post("/events/"+foreign+"/close",Map.of()).statusCode()).isEqualTo(403);accounts.assign(general,id,allowed,false);assertThat(b.get("/events/"+allowed+"/edit").statusCode()).isEqualTo(403);}}
 @Test void accountEventBindingIsUniqueAndNeverMergesPoints(){long a=account(address()),b=account(address()),e=event("Identidad");var f=new ParticipantForm();f.setAlias("Anterior");f.setEventId(e);long p=participants.create(f).id();accounts.bind(a,p,e);assertThatThrownBy(()->accounts.bind(b,p,e)).isInstanceOf(BusinessRuleException.class);long q=participants.create(f).id();assertThatThrownBy(()->accounts.bind(a,q,e)).isInstanceOf(BusinessRuleException.class);assertThat(participants.get(p).points()).isZero();}
 @Test void recoveryIsGenericAndResetInvalidatesOldSessionsAndCannotReplay()throws Exception{String email=address();account(email);try(var b=new TestBrowser(port);var recover=new TestBrowser(port)){login(b,email);recover.get("/account/recover");var absent=recover.post("/account/recover",Map.of("email",address()));var present=recover.post("/account/recover",Map.of("email",email));for(var result:List.of(absent,present))assertThat(result.body()).contains("Si existe una cuenta que pueda recuperarse");String t=token();accounts.consume(t,"RESET","Otra contraseña de prueba2026");assertThatThrownBy(()->accounts.consume(t,"RESET",PASSWORD)).isInstanceOf(BusinessRuleException.class);assertThat(b.get("/account").headers().firstValue("Location").orElse("")).contains("/login");}}
 @Test void expiredOrWrongPurposeTokensDoNotChangeAccounts(){String email=address();accounts.register(email,PASSWORD);String t=token();assertThatThrownBy(()->accounts.consume(t,"RESET",PASSWORD)).isInstanceOf(BusinessRuleException.class);jdbc.update("update account_tokens set expires_at=? where token_hash=?",java.sql.Timestamp.from(java.time.Instant.now().minusSeconds(60)),AccountService.hash(t));assertThatThrownBy(()->accounts.consume(t,"VERIFY","")).isInstanceOf(BusinessRuleException.class);assertThat(accounts.user(accounts.find(email).orElseThrow()).verified()).isFalse();}
 @Test void oauthNeverAutomaticallyLinksMatchingEmailsAndProviderIdentityIsUnique(){String email=address();long a=account(email),b=account(address());assertThatThrownBy(()->accounts.external("google","subject-"+UUID.randomUUID(),email,true,null)).isInstanceOf(BusinessRuleException.class);String subject="subject-"+UUID.randomUUID();assertThat(accounts.external("google",subject,email,true,a)).isEqualTo(a);assertThatThrownBy(()->accounts.external("google",subject,email,true,b)).isInstanceOf(BusinessRuleException.class);}
 @Test void missingProvidersHaveNoActiveButtonsAndReturnDestinationCannotBeExternal()throws Exception{String email=address();account(email);try(var b=new TestBrowser(port)){var page=b.get("/login?next=https://evil.example");assertThat(page.body()).doesNotContain("Continuar con Google","Continuar con Facebook");var result=b.post("/login",Map.of("email",email,"password",PASSWORD));var destination=java.net.URI.create(result.headers().firstValue("Location").orElseThrow());assertThat(destination.getPath()).isEqualTo("/join");assertThat(destination.getHost()).isIn(null,"127.0.0.1");}}
 @Test void repeatedAttemptsHaveRecoverableRateLimit(){String key=UUID.randomUUID().toString();throttle.check("test",key,1,1);assertThatThrownBy(()->throttle.check("test",key,1,1)).isInstanceOf(BusinessRuleException.class).hasMessageContaining("Espera");}
 @Test void liveStateAndHtmlStayPrivateAcrossConcurrentAccountsAndFreshRevocation()throws Exception{
  String first=address(),second=address();long a=account(first),b=account(second),e=event("Privado A"),f=event("Privado B");
  var p=new ParticipantForm();p.setAlias("Persona privada A");p.setEventId(e);long x=participants.create(p).id();accounts.bind(a,x,e);
  p.setAlias("Persona privada B");p.setEventId(f);long y=participants.create(p).id();accounts.bind(b,y,f);
  try(var one=new TestBrowser(port);var two=new TestBrowser(port);var executor=java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()){
   login(one,first);login(two,second);
   var left=executor.submit(()->one.get("/me/state?participantId="+y));
   var right=executor.submit(()->two.get("/me/state?participantId="+x));
   var leftState=left.get();var rightState=right.get();
   assertThat(leftState.statusCode()).isEqualTo(200);assertThat(rightState.statusCode()).isEqualTo(200);
   assertThat(leftState.body()).contains("\"signature\":\""+x+":").doesNotContain("Persona privada","recognitionHint");
   assertThat(rightState.body()).contains("\"signature\":\""+y+":");
   for(var response:List.of(leftState,rightState,one.get("/me",Map.of("Accept-Encoding","gzip")))){
    assertThat(response.headers().firstValue("Cache-Control").orElse("")).contains("no-store").doesNotContain("public","immutable");
    assertThat(response.headers().firstValue("Content-Encoding")).isEmpty();
   }
   assertThat(one.get("/events/"+f+"/plan").statusCode()).isEqualTo(403);
   events.transition(e,EventStatus.PAUSED);
   assertThat(one.get("/me/state").body()).contains(":PAUSED:");
   jdbc.update("update ping_accounts set auth_epoch=auth_epoch+1 where id=?",a);
   assertThat(one.get("/me/state").statusCode()).isEqualTo(401);
   assertThat(two.get("/me/state").statusCode()).isEqualTo(200);
  }
 }
 @Test void loginToNewEventDoesNotResumeAnUnrelatedParticipation()throws Exception{
  String email=address();long a=account(email),old=event("Anterior"),target=event("Destino");
  var f=new ParticipantForm();f.setAlias("Alias anterior privado");f.setEventId(old);long person=participants.create(f).id();accounts.bind(a,person,old);
  try(var b=new TestBrowser(port)){
   assertThat(b.get("/join?code="+events.get(target).getEntryCode()).statusCode()).isEqualTo(302);
   for(String route:List.of("/login","/register","/account/recover"))assertThat(b.get(route).body()).contains(events.get(target).getName());
   login(b,email);
   var response=b.get("/join");assertThat(response.statusCode()).isEqualTo(200);
   assertThat(response.body()).contains(events.get(target).getName()).doesNotContain("Alias anterior privado");
   assertThat(accounts.participant(a,target)).isEmpty();assertThat(participants.get(person).points()).isZero();
  }
 }
 @Test void loginResumesTheParticipationOfTheChosenEvent()throws Exception{
  String email=address();long a=account(email),first=event("Primero"),target=event("Elegido");
  for(long e:List.of(target,first)){var f=new ParticipantForm();f.setAlias(e==target?"Alias del elegido":"Alias del primero");f.setEventId(e);accounts.bind(a,participants.create(f).id(),e);}
  try(var b=new TestBrowser(port)){
   b.get("/join?code="+events.get(target).getEntryCode());login(b,email);
   assertThat(b.get("/join").headers().firstValue("Location").orElse("")).endsWith("/me");
   assertThat(b.get("/me").body()).contains("Alias del elegido").doesNotContain("Alias del primero");
  }
 }

 @Test void squatterOfAnUnverifiedEmailNeverGainsAccessAndTheOwnerSetsANewPassword()throws Exception{
  String email=address(),squatter="Contraseña del intruso2026",owner="Contraseña nueva del dueño2026";
  accounts.register(email,squatter);String registration=token();long id=accounts.find(email).orElseThrow();long epoch=accounts.user(id).epoch();
  // The owner receives the registration link but does not know the squatter's password: it cannot activate the account.
  assertThatThrownBy(()->accounts.consume(registration,"VERIFY",PASSWORD)).isInstanceOf(BusinessRuleException.class);assertThatThrownBy(()->accounts.consume(registration,"VERIFY","")).isInstanceOf(BusinessRuleException.class);
  assertThat(accounts.user(id).verified()).isFalse();
  // Re-registering or recovering an unverified address sends an activation link that requires a new password.
  accounts.register(email,owner);assertThat(mail.getLast().getSubject()).isEqualTo("Activa tu cuenta de PING");assertThat(mail.getLast().getText()).contains("/account/reset?token=");String stale=token();
  try(var b=new TestBrowser(port)){b.get("/account/recover");b.post("/account/recover",Map.of("email",email));}
  assertThat(mail.getLast().getSubject()).isEqualTo("Activa tu cuenta de PING");String activation=token();
  assertThatThrownBy(()->accounts.consume(stale,"RESET",owner)).isInstanceOf(BusinessRuleException.class);
  try(var b=new TestBrowser(port)){b.get("/account/reset?token="+activation);assertThat(b.post("/account/reset",Map.of("token",activation,"password",owner,"repeat",owner)).headers().firstValue("Location").orElse("")).endsWith("/join");assertThat(b.get("/account").body()).contains(email);}
  var user=accounts.user(id);assertThat(user.verified()).isTrue();assertThat(user.epoch()).isGreaterThan(epoch);assertThat(user.general()).isFalse();assertThat(accounts.managed(id)).isEmpty();
  // Every earlier link is void and single use: the squatter's registration link, the activation link itself.
  assertThatThrownBy(()->accounts.consume(registration,"VERIFY",squatter)).isInstanceOf(BusinessRuleException.class);assertThatThrownBy(()->accounts.consume(activation,"RESET",squatter)).isInstanceOf(BusinessRuleException.class);
  assertThat(jdbc.queryForObject("select count(*) from account_tokens where account_id=? and consumed=false",Integer.class,id)).isZero();
  try(var squatterBrowser=new TestBrowser(port);var ownerBrowser=new TestBrowser(port)){
   squatterBrowser.get("/login");assertThat(squatterBrowser.post("/login",Map.of("email",email,"password",squatter)).headers().firstValue("Location").orElse("")).contains("error");assertThat(squatterBrowser.get("/account").statusCode()).isEqualTo(302);
   ownerBrowser.get("/login");assertThat(ownerBrowser.post("/login",Map.of("email",email,"password",owner)).headers().firstValue("Location").orElse("")).doesNotContain("error");assertThat(ownerBrowser.get("/account").body()).contains(email);
  }
  assertThat(jdbc.queryForObject("select count(*) from external_identities where account_id=?",Integer.class,id)).isZero();
 }
 @Test void legitimateRegistrationStillActivatesWithItsOwnPasswordAndVerifiedRecoveryEndsSessions()throws Exception{
  String email=address();accounts.register(email,PASSWORD);assertThat(mail.getLast().getSubject()).isEqualTo("Verifica tu correo de PING");String t=token();long id=accounts.find(email).orElseThrow();
  try(var b=new TestBrowser(port)){assertThat(b.get("/account/verify?token="+t).body()).contains("name=\"password\"");var wrong=b.post("/account/verify",Map.of("token",t,"password","otra contraseña cualquiera"));assertThat(wrong.body()).contains("Esa no es la contraseña");assertThat(b.post("/account/verify",Map.of("token",t,"password",PASSWORD)).headers().firstValue("Location").orElse("")).endsWith("/join");assertThat(b.get("/account").statusCode()).isEqualTo(200);
   accounts.recover(email);assertThat(mail.getLast().getSubject()).isEqualTo("Recupera tu acceso a PING");accounts.consume(token(),"RESET","Otra contraseña de prueba2026");assertThat(b.get("/account").statusCode()).isEqualTo(302);}
  assertThat(accounts.user(id).verified()).isTrue();
 }

 String location(java.net.http.HttpResponse<?> r){var uri=java.net.URI.create(r.headers().firstValue("Location").orElse(""));return uri.getRawPath()+(uri.getRawQuery()==null?"":"?"+uri.getRawQuery());}
 Map<String,String> draft(String html){var data=new HashMap<String,String>();for(String name:List.of("draftKey","revision")){var m=Pattern.compile("name=\""+name+"\"[^>]*value=\"([^\"]*)\"").matcher(html);assertThat(m.find()).isTrue();data.put(name,m.group(1));}return data;}
 @Test void registrationFromAnEventContinuesSignedInWhereTheLinkIsOpenedWithoutAnotherLogin()throws Exception{
  String email=address();long e=event("Registro continuo");String code=events.get(e).getEntryCode();
  try(var b=new TestBrowser(port)){
   assertThat(location(b.get("/join?code="+code))).isEqualTo("/login");b.get("/register");
   assertThat(location(b.post("/register",Map.of("email",email,"password",PASSWORD,"repeat",PASSWORD)))).isEqualTo("/register/check");
   var check=b.get("/register/check");assertThat(check.body()).contains("Revisa tu correo",email,events.get(e).getName()).doesNotContain(PASSWORD);
   int sent=mail.size();assertThat(b.get("/register/check").statusCode()).isEqualTo(200);assertThat(mail).hasSize(sent);
   assertThat(b.get("/account").statusCode()).isEqualTo(302);
   String t=token();b.get("/account/verify?token="+t);
   assertThat(location(b.post("/account/verify",Map.of("token",t,"password",PASSWORD)))).isEqualTo("/join");
   var details=b.get("/join");assertThat(details.statusCode()).isEqualTo(200);assertThat(details.body()).contains(events.get(e).getName());
   var f=draft(details.body());f.put("alias","Recién registrada");f.put("eventId",""+e);assertThat(location(b.post("/join",f))).isEqualTo("/join/interests");
   f=draft(b.get("/join/interests").body());f.put("interestIds",""+interests.catalog().getFirst().getId());assertThat(location(b.post("/join/interests",f))).isEqualTo("/me");
  }
  long id=accounts.find(email).orElseThrow();assertThat(accounts.user(id).verified()).isTrue();assertThat(accounts.user(id).general()).isFalse();assertThat(accounts.participant(id,e)).isPresent();
 }
 @Test void verifyingInAnotherBrowserSignsInThereButNeverTheRegisteringBrowser()throws Exception{
  String email=address();
  try(var registering=new TestBrowser(port);var mailbox=new TestBrowser(port)){
   registering.get("/register");registering.post("/register",Map.of("email",email,"password",PASSWORD,"repeat",PASSWORD));String t=token();
   mailbox.get("/account/verify?token="+t);assertThat(location(mailbox.post("/account/verify",Map.of("token",t,"password",PASSWORD)))).isEqualTo("/join");assertThat(mailbox.get("/account").statusCode()).isEqualTo(200);
   assertThat(registering.get("/account").statusCode()).isEqualTo(302);assertThat(registering.get("/register/check").body()).contains("Entra con este correo");
  }
 }
 @Test void registeringAnExistingAddressNeitherDuplicatesNorChangesItAndHelpsTheOwnerIn()throws Exception{
  String email=address();long id=account(email);String hash=accounts.user(id).passwordHash();long epoch=accounts.user(id).epoch();
  try(var b=new TestBrowser(port)){
   b.get("/register");assertThat(location(b.post("/register",Map.of("email",email,"password","Otra contraseña distinta2026","repeat","Otra contraseña distinta2026")))).isEqualTo("/register/check");
   assertThat(b.get("/register/check").body()).contains("Revisa tu correo");assertThat(mail.getLast().getSubject()).isEqualTo("Ya tienes una cuenta de PING");
   assertThat(b.get("/login").body()).contains("value=\""+email+"\"");
  }
  assertThat(jdbc.queryForObject("select count(*) from ping_accounts where email=?",Integer.class,email)).isOne();var user=accounts.user(id);assertThat(user.passwordHash()).isEqualTo(hash);assertThat(user.epoch()).isEqualTo(epoch);assertThat(user.verified()).isTrue();
 }
 @Test void resendReplacesThePreviousLinkAndAddressCanBeCorrected()throws Exception{
  String email=address();
  try(var b=new TestBrowser(port)){
   b.get("/register");b.post("/register",Map.of("email",email,"password",PASSWORD,"repeat",PASSWORD));String first=token();
   b.get("/register/check");assertThat(location(b.post("/register/resend",Map.of()))).isEqualTo("/register/check?resent=true");String second=token();assertThat(second).isNotEqualTo(first);
   assertThatThrownBy(()->accounts.consume(first,"VERIFY",PASSWORD)).isInstanceOf(BusinessRuleException.class);
   assertThat(b.get("/register").body()).contains("value=\""+email+"\"");
   accounts.consume(second,"VERIFY",PASSWORD);
  }
  assertThat(accounts.user(accounts.find(email).orElseThrow()).verified()).isTrue();
 }
 @Test void failedLoginKeepsTheTypedEmailAndOffersSignUpWithoutCreatingAnything()throws Exception{
  String email=address();int before=jdbc.queryForObject("select count(*) from ping_accounts",Integer.class);
  try(var b=new TestBrowser(port)){
   b.get("/login");assertThat(location(b.post("/login",Map.of("email",email,"password",PASSWORD)))).isEqualTo("/login?error=true");
   var page=b.get("/login?error=true").body();assertThat(page).contains("Créala con este correo","value=\""+email+"\"").doesNotContain(PASSWORD);
   assertThat(b.get("/register").body()).contains("value=\""+email+"\"");
  }
  assertThat(jdbc.queryForObject("select count(*) from ping_accounts",Integer.class)).isEqualTo(before);assertThat(accounts.find(email)).isEmpty();
 }
 @Test void accountMailIsMultipartWithTheSameActionAndThePublicLogoNeedsNoSession()throws Exception{
  String email=address();accounts.register(email,PASSWORD);var sent=mail.getLast();String t=token();String url="http://127.0.0.1:8090/account/verify?token="+t;
  assertThat(sent.to()).isEqualTo(email);assertThat(sent.getSubject()).isEqualTo("Verifica tu correo de PING");
  assertThat(sent.text()).contains("Verificar mi correo:\n"+url,"24 horas");assertThat(sent.html()).contains("href=\""+url+"\"",">Verificar mi correo</a>","http://127.0.0.1:8090/img/email/ping-logo.png");
  try(var b=new TestBrowser(port)){var logo=b.bytes("/img/email/ping-logo.png");assertThat(logo.statusCode()).isEqualTo(200);assertThat(logo.headers().firstValue("Content-Type").orElse("")).startsWith("image/png");assertThat(logo.body().length).isGreaterThan(1000);}
 }
 @Test void authenticatedPagesRefreshTheAccountOnlyOncePerRequest()throws Exception{
  String email=address();long a=account(email),e=event("Lectura acotada");
  var p=new ParticipantForm();p.setAlias("Persona de ensayo");p.setEventId(e);accounts.bind(a,participants.create(p).id(),e);
  try(var b=new TestBrowser(port)){
   login(b,email);
   var checks=new org.assertj.core.api.SoftAssertions();
   for(String path:List.of("/","/account","/me/state")){
    clearInvocations(accounts);
    assertThat(b.get(path).statusCode()).isEqualTo(200);
    long reads=mockingDetails(accounts).getInvocations().stream().filter(i->i.getMethod().getName().equals("user")).count();
    System.out.println("PERF account_reads "+path+"="+reads);
    checks.assertThat(reads).as("fresh account reads for %s",path).isEqualTo(1);
   }
   checks.assertAll();
  }
 }
 @Test void publicClasspathAssetsDoNotReadAccountsEvenWithASessionCookie()throws Exception{
  String email=address();account(email);
  try(var b=new TestBrowser(port)){
   login(b,email);
   var checks=new org.assertj.core.api.SoftAssertions();
   for(String path:List.of("/css/brand.css","/js/ui.js","/img/favicon.svg")){
    clearInvocations(accounts);
    assertThat(b.get(path).statusCode()).isEqualTo(200);
    long reads=mockingDetails(accounts).getInvocations().stream().filter(i->i.getMethod().getName().equals("user")).count();
    System.out.println("PERF account_reads "+path+"="+reads);
    checks.assertThat(reads).as("account reads for public asset %s",path).isZero();
   }
   checks.assertAll();
  }
 }
}
