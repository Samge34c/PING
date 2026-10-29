package com.ping.auth;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.*;
import com.ping.event.*;
import com.ping.interest.InterestService;
import com.ping.support.TestBrowser;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.*;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;
/**
 * Full Spring Security OIDC sign-in against a LOCAL stand-in for Google's token/JWKS/userinfo endpoints:
 * real state/nonce checks, RS256 ID-token signature, client authentication and single-use codes.
 * It does not contact Google and does not prove the Google Cloud configuration or consent screen.
 */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"spring.datasource.url=jdbc:h2:mem:oauth-flow;MODE=MySQL;DB_CLOSE_DELAY=-1","spring.datasource.username=sa","spring.datasource.password=","spring.jpa.hibernate.ddl-auto=validate","ping.oauth.enabled=true","PING_GOOGLE_CLIENT_ID="+OAuthLoginFlowTests.CLIENT,"PING_GOOGLE_CLIENT_SECRET="+OAuthLoginFlowTests.SECRET,"ping.mail.from=ping@example.test","ping.public-base-url=http://127.0.0.1:8090"}) @ActiveProfiles("security-test")
class OAuthLoginFlowTests{
 static final String CLIENT="ping-local-test-client",SECRET="local-test-not-a-credential",PASSWORD="Prueba local de cuenta2026";
 static final FakeGoogle google=new FakeGoogle();
 @TestConfiguration static class Provider{@Bean @Primary ClientRegistrationRepository fakeGoogle(){return new InMemoryClientRegistrationRepository(CommonOAuth2Provider.GOOGLE.getBuilder("google").clientId(CLIENT).clientSecret(SECRET).scope("openid","email","profile").redirectUri("{baseUrl}/login/oauth2/code/{registrationId}").issuerUri(google.base).tokenUri(google.base+"/token").jwkSetUri(google.base+"/jwks").userInfoUri(google.base+"/userinfo").build());}}
 @org.springframework.test.context.bean.override.mockito.MockitoBean org.springframework.mail.javamail.JavaMailSender sender;final List<com.ping.support.MailCapture.Mail> mail=new java.util.concurrent.CopyOnWriteArrayList<>();
 @BeforeEach void transport(){mail.clear();com.ping.support.MailCapture.capture(sender,mail);}
 String token(){var m=java.util.regex.Pattern.compile("token=([A-Za-z0-9_-]{43})").matcher(mail.getLast().getText());assertThat(m.find()).isTrue();return m.group(1);}
 @Autowired AccountService accounts;@Autowired EventService events;@Autowired InterestService interests;@Autowired JdbcTemplate jdbc;@Autowired PasswordEncoder encoder;@LocalServerPort int port;
 @AfterAll static void stop(){google.server.stop(0);}
 String address(){return "persona-"+UUID.randomUUID()+"@example.test";}
 Event event(String name){var f=new EventForm();f.setName(name+" "+UUID.randomUUID().toString().substring(0,6));var e=events.create(f);events.transition(e.getId(),EventStatus.OPEN);return events.get(e.getId());}
 long local(String email){jdbc.update("insert into ping_accounts(email,password_hash,email_verified) values(?,?,true)",email,encoder.encode(PASSWORD));return accounts.find(email).orElseThrow();}
 int count(String sql,Object... args){return jdbc.queryForObject(sql,Integer.class,args);}
 static String location(java.net.http.HttpResponse<?> r){var uri=URI.create(r.headers().firstValue("Location").orElse(""));return uri.getRawPath()+(uri.getRawQuery()==null?"":"?"+uri.getRawQuery());}
 static Map<String,String> query(String location){var out=new HashMap<String,String>();for(String pair:URI.create(location).getRawQuery().split("&")){int i=pair.indexOf('=');out.put(pair.substring(0,i),URLDecoder.decode(pair.substring(i+1),StandardCharsets.UTF_8));}return out;}
 /** Click «Continuar con Google», "consent" at the stand-in provider and follow the callback. */
 java.net.http.HttpResponse<String> google(TestBrowser b,String subject,String email,boolean verified)throws Exception{
  var start=b.get("/oauth2/authorization/google");assertThat(start.statusCode()).isEqualTo(302);var q=query(start.headers().firstValue("Location").orElseThrow());
  return b.get("/login/oauth2/code/google?code="+google.issue(subject,email,verified,q.get("nonce"),q.get("redirect_uri"))+"&state="+URLEncoder.encode(q.get("state"),StandardCharsets.UTF_8));
 }
 Map<String,String> fields(String html){var data=new HashMap<String,String>();for(String name:List.of("draftKey","revision")){var m=java.util.regex.Pattern.compile("name=\""+name+"\"[^>]*value=\"([^\"]*)\"").matcher(html);assertThat(m.find()).isTrue();data.put(name,m.group(1));}return data;}

 @Test void newGoogleUserKeepsTheChosenEventCompletesPingProfileAndGetsOnlyBasicPermissions()throws Exception{
  var e=event("Festival QR");String email=address(),subject="g-"+UUID.randomUUID();
  try(var b=new TestBrowser(port)){
   assertThat(location(b.get("/join?code="+e.getEntryCode()))).isEqualTo("/login");
   assertThat(b.get("/login").body()).contains("Continuar con Google",e.getName(),"o con tu correo");
   var callback=google(b,subject,email,true);
   assertThat(location(callback)).isEqualTo("/join");
   assertThat(callback.headers().allValues("Set-Cookie")).anyMatch(c->c.startsWith("JSESSIONID="));
   var details=b.get("/join");assertThat(details.statusCode()).isEqualTo(200);assertThat(details.body()).contains(e.getName());
   var f=fields(details.body());f.put("alias","Alias elegido en PING");f.put("eventId",""+e.getId());assertThat(location(b.post("/join",f))).isEqualTo("/join/interests");
   f=fields(b.get("/join/interests").body());f.put("interestIds",""+interests.catalog().getFirst().getId());assertThat(location(b.post("/join/interests",f))).isEqualTo("/me");
   assertThat(b.get("/me").body()).contains("Alias elegido en PING");
   assertThat(b.get("/organizer").statusCode()).isEqualTo(403);assertThat(b.get("/organizer/access").statusCode()).isEqualTo(403);
  }
  long id=accounts.find(email).orElseThrow();var user=accounts.user(id);
  assertThat(user.verified()).isTrue();assertThat(user.general()).isFalse();assertThat(user.organizer()).isFalse();
  assertThat(count("select count(*) from external_identities where provider='google' and subject=? and account_id=?",subject,id)).isOne();
  assertThat(count("select count(*) from account_participations where account_id=? and event_id=?",id,e.getId())).isOne();
 }

 @Test void returningIdentityEntersTheSameAccountWithoutDuplicatesOrOverwritingTheAlias()throws Exception{
  var e=event("Regreso");String email=address(),subject="g-"+UUID.randomUUID();
  try(var b=new TestBrowser(port)){b.get("/join?code="+e.getEntryCode());google(b,subject,email,true);var f=fields(b.get("/join").body());f.put("alias","Alias original");f.put("eventId",""+e.getId());b.post("/join",f);f=fields(b.get("/join/interests").body());f.put("interestIds",""+interests.catalog().getFirst().getId());b.post("/join/interests",f);}
  long id=accounts.find(email).orElseThrow();
  try(var again=new TestBrowser(port)){
   again.get("/login");var callback=google(again,subject,email,true);assertThat(location(callback)).isEqualTo("/me");
   assertThat(again.get("/me").body()).contains("Alias original").doesNotContain(FakeGoogle.NAME);
   // Replaying the same callback (back button, double tap, repeated delivery) cannot create anything.
   var uri=callback.request().uri();assertThat(location(again.get(uri.getRawPath()+"?"+uri.getRawQuery()))).isEqualTo("/login?oauthError=expired&provider=google");
  }
  assertThat(count("select count(*) from ping_accounts where email=?",email)).isOne();
  assertThat(count("select count(*) from external_identities where subject=?",subject)).isOne();
  assertThat(count("select count(*) from account_participations where account_id=?",id)).isOne();
 }

 @Test void repeatedOrForgedCallbacksAndCancellationExplainTheOutcomeAndCreateNothing()throws Exception{
  String email=address();
  try(var b=new TestBrowser(port)){
   b.get("/login");var start=b.get("/oauth2/authorization/google");var q=query(start.headers().firstValue("Location").orElseThrow());String state=URLEncoder.encode(q.get("state"),StandardCharsets.UTF_8);
   var cancelled=b.get("/login/oauth2/code/google?error=access_denied&state="+state);assertThat(location(cancelled)).isEqualTo("/login?oauthError=cancelled&provider=google");
   assertThat(b.get(location(cancelled)).body()).contains("Cancelaste el acceso con Google");
   var replay=b.get("/login/oauth2/code/google?code=x&state="+state);assertThat(location(replay)).isEqualTo("/login?oauthError=expired&provider=google");
   assertThat(b.get(location(replay)).body()).contains("El acceso con Google caducó");
   b.get("/oauth2/authorization/google");var forged=b.get("/login/oauth2/code/google?code=x&state=forged");assertThat(location(forged)).isEqualTo("/login?oauthError=expired&provider=google");
   start=b.get("/oauth2/authorization/google");q=query(start.headers().firstValue("Location").orElseThrow());
   var rejected=b.get("/login/oauth2/code/google?code=unknown-code&state="+URLEncoder.encode(q.get("state"),StandardCharsets.UTF_8));assertThat(location(rejected)).isEqualTo("/login?oauthError=provider&provider=google");
   assertThat(b.get("/account").statusCode()).isEqualTo(302);
   var unverified=google(b,"g-"+UUID.randomUUID(),email,false);assertThat(location(unverified)).isEqualTo("/login?sent=true");assertThat(mail.getLast().getSubject()).isEqualTo("Activa tu cuenta de PING");assertThat(b.get("/account").statusCode()).isEqualTo(302);
  }
  assertThat(accounts.user(accounts.find(email).orElseThrow()).verified()).isFalse();
 }

 @Test void matchingEmailIsConfirmedInOneStepThenLinkedAndTheChosenEventContinues()throws Exception{
  var e=event("Vincular");String email=address(),subject="g-"+UUID.randomUUID();long existing=local(email);String hash=accounts.user(existing).passwordHash();
  try(var b=new TestBrowser(port)){
   b.get("/join?code="+e.getEntryCode());b.get("/login");
   assertThat(location(google(b,subject,email,true))).isEqualTo("/login/link");
   var page=b.get("/login/link");assertThat(page.body()).contains("Confirma que es tu cuenta",email,e.getName(),"Vincular Google y continuar","Entrar sin vincular");
   assertThat(b.get("/login/link").statusCode()).isEqualTo(200);assertThat(b.get("/account").statusCode()).isEqualTo(302);
   assertThat(count("select count(*) from external_identities where subject=?",subject)).isZero();
   var wrong=b.post("/login/link",Map.of("password","no es la contraseña","action","link"));assertThat(wrong.statusCode()).isEqualTo(200);assertThat(wrong.body()).contains("La contraseña no es correcta");
   assertThat(count("select count(*) from external_identities where subject=?",subject)).isZero();assertThat(b.get("/account").statusCode()).isEqualTo(302);
   var confirmed=b.post("/login/link",Map.of("password",PASSWORD,"action","link"));assertThat(location(confirmed)).isEqualTo("/join");
   assertThat(confirmed.headers().allValues("Set-Cookie")).anyMatch(c->c.startsWith("JSESSIONID="));
   assertThat(b.get("/join").body()).contains(e.getName());
   // Double tap / repeated POST: nothing new, still signed in.
   assertThat(location(b.post("/login/link",Map.of("password",PASSWORD,"action","link")))).isEqualTo("/me");
  }
  assertThat(count("select count(*) from external_identities where subject=? and account_id=?",subject,existing)).isOne();
  assertThat(count("select count(*) from ping_accounts where email=?",email)).isOne();assertThat(accounts.user(existing).passwordHash()).isEqualTo(hash);assertThat(accounts.user(existing).general()).isFalse();
  try(var later=new TestBrowser(port)){later.get("/join?code="+e.getEntryCode());assertThat(location(google(later,subject,email,true))).isEqualTo("/join");}
 }

 @Test void enteringWithoutLinkingSignsInButCreatesNoIdentityAndAnotherAccountCannotConsumeTheIntent()throws Exception{
  String email=address(),other=address(),subject="g-"+UUID.randomUUID();local(email);local(other);
  try(var b=new TestBrowser(port)){b.get("/login");google(b,subject,email,true);b.get("/login/link");
   assertThat(location(b.post("/login/link",Map.of("password",PASSWORD,"action","skip")))).isEqualTo("/join");assertThat(b.get("/account").body()).contains(email);}
  assertThat(count("select count(*) from external_identities where subject=?",subject)).isZero();
  try(var b=new TestBrowser(port)){b.get("/login");google(b,subject,email,true);b.get("/login");
   // A sign-in with a different account in the same session drops the intent instead of linking it there.
   assertThat(location(b.post("/login",Map.of("email",other,"password",PASSWORD)))).isEqualTo("/join");
   assertThat(location(b.get("/login/link"))).isEqualTo("/login?oauthError=expired&provider=google");}
  assertThat(count("select count(*) from external_identities where subject=?",subject)).isZero();
  assertThat(new AccessContinuation.PendingLink("google",subject,email,1,Instant.now().minusSeconds(1)).expired()).isTrue();
 }

 @Test void squatterPasswordNeverConfirmsAndTheOwnerRecoversThenLinksInTheSameJourney()throws Exception{
  var e=event("Recuperar");String email=address(),subject="g-"+UUID.randomUUID(),squatter="Contraseña del intruso2026",owner="Contraseña nueva del dueño2026";
  jdbc.update("insert into ping_accounts(email,password_hash,email_verified) values(?,?,false)",email,encoder.encode(squatter));long id=accounts.find(email).orElseThrow();
  try(var b=new TestBrowser(port)){
   b.get("/join?code="+e.getEntryCode());b.get("/login");assertThat(location(google(b,subject,email,true))).isEqualTo("/login/link");b.get("/login/link");
   assertThat(b.post("/login/link",Map.of("password",squatter,"action","link")).body()).contains("aún no está activada");assertThat(b.get("/account").statusCode()).isEqualTo(302);
   assertThat(b.get("/account/recover").body()).contains("value=\""+email+"\"");b.post("/account/recover",Map.of("email",email));assertThat(mail.getLast().getSubject()).isEqualTo("Activa tu cuenta de PING");String t=token();
   b.get("/account/reset?token="+t);assertThat(location(b.post("/account/reset",Map.of("token",t,"password",owner,"repeat",owner)))).isEqualTo("/login/link");
   var confirm=b.get("/login/link").body();assertThat(confirm).contains("Vincula Google","Vincular Google y continuar").doesNotContain("name=\"password\"");
   assertThat(location(b.post("/login/link",Map.of("action","link")))).isEqualTo("/join");assertThat(b.get("/join").body()).contains(e.getName());
  }
  assertThat(count("select count(*) from external_identities where subject=? and account_id=?",subject,id)).isOne();assertThat(accounts.user(id).verified()).isTrue();
  try(var squatterBrowser=new TestBrowser(port)){squatterBrowser.get("/login");assertThat(location(squatterBrowser.post("/login",Map.of("email",email,"password",squatter)))).isEqualTo("/login?error=true");}
 }

 @Test void cancelledOrRejectedLinkKeepsTheLocalSessionAndChangesNothing()throws Exception{
  String email=address(),takenSubject="g-"+UUID.randomUUID();local(email);long other=local(address());accounts.external("google",takenSubject,"x-"+UUID.randomUUID()+"@example.test",true,other);
  try(var b=new TestBrowser(port)){
   b.get("/login");b.post("/login",Map.of("email",email,"password",PASSWORD));b.get("/account");
   b.post("/account/link/google",Map.of());var start=b.get("/oauth2/authorization/google");var q=query(start.headers().firstValue("Location").orElseThrow());
   assertThat(location(b.get("/login/oauth2/code/google?error=access_denied&state="+URLEncoder.encode(q.get("state"),StandardCharsets.UTF_8)))).isEqualTo("/account?linkError=cancelled&provider=google");
   assertThat(b.get("/account?linkError=cancelled&provider=google").body()).contains(email,"Cancelaste la vinculación");
   b.post("/account/link/google",Map.of());assertThat(location(google(b,takenSubject,email,true))).isEqualTo("/account?linkError=taken&provider=google");
   assertThat(b.get("/account").body()).contains(email);
  }
  assertThat(count("select count(*) from external_identities where subject=? and account_id=?",takenSubject,other)).isOne();
 }

 @Test void enteringAnotherEventNeverReusesTheParticipationOfAPreviousEvent()throws Exception{
  var first=event("Primero");var second=event("Segundo");String email=address(),subject="g-"+UUID.randomUUID();
  try(var b=new TestBrowser(port)){b.get("/join?code="+first.getEntryCode());google(b,subject,email,true);var f=fields(b.get("/join").body());f.put("alias","Solo primero");f.put("eventId",""+first.getId());b.post("/join",f);f=fields(b.get("/join/interests").body());f.put("interestIds",""+interests.catalog().getFirst().getId());b.post("/join/interests",f);}
  try(var b=new TestBrowser(port)){
   b.get("/join?code="+second.getEntryCode());assertThat(location(google(b,subject,email,true))).isEqualTo("/join");
   var page=b.get("/join");assertThat(page.statusCode()).isEqualTo(200);assertThat(page.body()).contains(second.getName()).doesNotContain("Solo primero");
  }
 }

 @Test void generalAdministratorKeepsPermissionsWithGoogleAndGoogleNeverGrantsThem()throws Exception{
  String email="general-oauth-"+UUID.randomUUID()+"@example.test";long general=local(email);jdbc.update("update ping_accounts set general_admin=true where id=?",general);
  String subject="g-"+UUID.randomUUID();accounts.external("google",subject,email,true,general);
  try(var b=new TestBrowser(port)){b.get("/login?next=/organizer");assertThat(location(google(b,subject,email,true))).isEqualTo("/organizer");assertThat(b.get("/organizer").statusCode()).isEqualTo(200);assertThat(b.get("/organizer/access").statusCode()).isEqualTo(200);}
  try(var b=new TestBrowser(port)){b.get("/login?next=/organizer");google(b,"g-"+UUID.randomUUID(),"admin-"+UUID.randomUUID()+"@example.test",true);assertThat(b.get("/organizer").statusCode()).isEqualTo(403);}
 }

 @Test void concurrentCallbacksForTheSameNewIdentityCreateOneAccount()throws Exception{
  String email=address(),subject="g-"+UUID.randomUUID();var pool=Executors.newFixedThreadPool(6);var ready=new CountDownLatch(1);
  try{var tasks=new ArrayList<Future<Long>>();for(int i=0;i<6;i++)tasks.add(pool.submit(()->{ready.await();return accounts.external("google",subject,email,true,null);}));ready.countDown();
   var ids=new HashSet<Long>();for(var t:tasks)ids.add(t.get(30,TimeUnit.SECONDS));assertThat(ids).hasSize(1);}finally{pool.shutdownNow();}
  assertThat(count("select count(*) from ping_accounts where email=?",email)).isOne();assertThat(count("select count(*) from external_identities where subject=?",subject)).isOne();
 }

 /** Local stand-in for Google's token, JWKS and userinfo endpoints. Authorization itself is answered by the test. */
 static class FakeGoogle{
  static final String NAME="Nombre De Google";
  final HttpServer server;final String base;final RSAKey key;
  record Grant(String subject,String email,boolean verified,String nonce,String redirect){}
  final Map<String,Grant> codes=new ConcurrentHashMap<>(),tokens=new ConcurrentHashMap<>();
  FakeGoogle(){
   try{key=new RSAKeyGenerator(2048).keyID("ping-test").generate();server=HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(),0),0);}catch(Exception e){throw new IllegalStateException(e);}
   base="http://127.0.0.1:"+server.getAddress().getPort();
   server.createContext("/jwks",x->send(x,200,new JWKSet(key.toPublicJWK()).toString()));
   server.createContext("/token",x->{
    var form=new HashMap<String,String>();for(String p:new String(x.getRequestBody().readAllBytes(),StandardCharsets.UTF_8).split("&")){int i=p.indexOf('=');if(i>0)form.put(p.substring(0,i),URLDecoder.decode(p.substring(i+1),StandardCharsets.UTF_8));}
    String basic="Basic "+Base64.getEncoder().encodeToString((CLIENT+":"+SECRET).getBytes(StandardCharsets.UTF_8));
    Grant g=codes.remove(Objects.toString(form.get("code"),""));
    if(!basic.equals(x.getRequestHeaders().getFirst("Authorization"))||g==null||!"authorization_code".equals(form.get("grant_type"))||!g.redirect().equals(form.get("redirect_uri"))){send(x,400,"{\"error\":\"invalid_grant\"}");return;}
    try{var now=Instant.now();var claims=new JWTClaimsSet.Builder().issuer(base).subject(g.subject()).audience(CLIENT).issueTime(Date.from(now)).expirationTime(Date.from(now.plusSeconds(300))).claim("nonce",g.nonce()).claim("email",g.email()).claim("email_verified",g.verified()).build();
     var jwt=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(),claims);jwt.sign(new RSASSASigner(key));
     String access=UUID.randomUUID().toString();tokens.put(access,g);
     send(x,200,"{\"access_token\":\""+access+"\",\"token_type\":\"Bearer\",\"expires_in\":300,\"scope\":\"openid email profile\",\"id_token\":\""+jwt.serialize()+"\"}");}catch(JOSEException e){send(x,500,"{}");}
   });
   server.createContext("/userinfo",x->{String auth=Objects.toString(x.getRequestHeaders().getFirst("Authorization"),"");Grant g=tokens.get(auth.replaceFirst("^Bearer ",""));if(g==null){send(x,401,"{}");return;}
    send(x,200,"{\"sub\":\""+g.subject()+"\",\"email\":\""+g.email()+"\",\"email_verified\":"+g.verified()+",\"name\":\""+NAME+"\",\"picture\":\"https://example.test/google.png\"}");});
   server.start();
  }
  String issue(String subject,String email,boolean verified,String nonce,String redirect){String code=UUID.randomUUID().toString();codes.put(code,new Grant(subject,email,verified,nonce,redirect));return code;}
  static void send(com.sun.net.httpserver.HttpExchange x,int status,String body)throws java.io.IOException{byte[] bytes=body.getBytes(StandardCharsets.UTF_8);x.getResponseHeaders().set("Content-Type","application/json");x.sendResponseHeaders(status,bytes.length);try(var out=x.getResponseBody()){out.write(bytes);}}
 }
}
