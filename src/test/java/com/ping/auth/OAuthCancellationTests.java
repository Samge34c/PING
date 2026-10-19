package com.ping.auth;
import java.net.*;
import java.util.*;
import com.ping.support.TestBrowser;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;
/** Exercises Spring's redirect/state/error path only. These are NOT provider credentials or a successful OAuth login. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"spring.datasource.url=jdbc:h2:mem:oauth-cancel;MODE=MySQL;DB_CLOSE_DELAY=-1","spring.datasource.username=sa","spring.datasource.password=","spring.jpa.hibernate.ddl-auto=validate","ping.oauth.enabled=true","PING_GOOGLE_CLIENT_ID=local-test-no-provider","PING_GOOGLE_CLIENT_SECRET=local-test-not-a-credential"}) @ActiveProfiles("security-test")
class OAuthCancellationTests{
 @LocalServerPort int port;
 @Test void cancellationAndMissingStateReturnRecoverableFailureWithoutIdentity()throws Exception{
  try(var b=new TestBrowser(port)){
   b.get("/login");var start=b.get("/oauth2/authorization/google");assertThat(start.statusCode()).isEqualTo(302);URI uri=URI.create(start.headers().firstValue("Location").orElseThrow());assertThat(uri.getHost()).isEqualTo("accounts.google.com");
   String state=Arrays.stream(uri.getRawQuery().split("&")).filter(x->x.startsWith("state=")).findFirst().orElseThrow().substring(6);
   var cancelled=b.get("/login/oauth2/code/google?error=access_denied&state="+state);assertThat(cancelled.headers().firstValue("Location").orElse("")).endsWith("/login?oauthError=true");assertThat(b.get("/account").statusCode()).isEqualTo(302);
   var invalid=b.get("/login/oauth2/code/google?error=access_denied&state=missing");assertThat(invalid.headers().firstValue("Location").orElse("")).endsWith("/login?oauthError=true");
  }
 }
}
