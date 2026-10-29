package com.ping.auth;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.ping.support.TestBrowser;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;
/** Exercises the production Google registration's redirect/state/error path only. These are NOT provider credentials or a successful OAuth login. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"spring.datasource.url=jdbc:h2:mem:oauth-cancel;MODE=MySQL;DB_CLOSE_DELAY=-1","spring.datasource.username=sa","spring.datasource.password=","spring.jpa.hibernate.ddl-auto=validate","ping.oauth.enabled=true","PING_GOOGLE_CLIENT_ID=local-test-no-provider","PING_GOOGLE_CLIENT_SECRET=local-test-not-a-credential","ping.public-base-url=https://ping.example.test/"}) @ActiveProfiles("security-test")
class OAuthCancellationTests{
 @LocalServerPort int port;
 @Test void cancellationAndMissingStateReturnRecoverableFailureWithoutIdentity()throws Exception{
  try(var b=new TestBrowser(port)){
   assertThat(b.get("/register").body()).contains("Continuar con Google");
   b.get("/login");var start=b.get("/oauth2/authorization/google");assertThat(start.statusCode()).isEqualTo(302);URI uri=URI.create(start.headers().firstValue("Location").orElseThrow());assertThat(uri.getHost()).isEqualTo("accounts.google.com");
   var q=new HashMap<String,String>();for(String pair:uri.getRawQuery().split("&")){int i=pair.indexOf('=');q.put(pair.substring(0,i),URLDecoder.decode(pair.substring(i+1),StandardCharsets.UTF_8));}
   // Identity only, OIDC nonce, and the exact callback registered in Google (from PING_PUBLIC_BASE_URL, not proxy headers).
   assertThat(q.get("scope").split(" ")).containsExactlyInAnyOrder("openid","email","profile");assertThat(q.get("response_type")).isEqualTo("code");assertThat(q).containsKeys("nonce","state");
   assertThat(q.get("redirect_uri")).isEqualTo("https://ping.example.test/login/oauth2/code/google");assertThat(uri.getRawQuery()).doesNotContain("local-test-not-a-credential");
   String state=URLEncoder.encode(q.get("state"),StandardCharsets.UTF_8);
   var cancelled=b.get("/login/oauth2/code/google?error=access_denied&state="+state);assertThat(cancelled.headers().firstValue("Location").orElse("")).endsWith("/login?oauthError=cancelled&provider=google");assertThat(b.get("/account").statusCode()).isEqualTo(302);
   var invalid=b.get("/login/oauth2/code/google?error=access_denied&state=missing");assertThat(invalid.headers().firstValue("Location").orElse("")).endsWith("/login?oauthError=expired&provider=google");
  }
 }
}
