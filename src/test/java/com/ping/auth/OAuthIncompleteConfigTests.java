package com.ping.auth;
import com.ping.support.TestBrowser;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;
/** PING_OAUTH_ENABLED=true with an incomplete Google pair must start with e-mail access only, never a dead button. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"spring.datasource.url=jdbc:h2:mem:oauth-incomplete;MODE=MySQL;DB_CLOSE_DELAY=-1","spring.datasource.username=sa","spring.datasource.password=","spring.jpa.hibernate.ddl-auto=validate","ping.oauth.enabled=true","PING_GOOGLE_CLIENT_ID=only-the-id"}) @ActiveProfiles("security-test")
class OAuthIncompleteConfigTests{
 @LocalServerPort int port;
 @Test void incompleteProviderIsHiddenAndEmailAccessRemains()throws Exception{
  try(var b=new TestBrowser(port)){
   var login=b.get("/login");assertThat(login.statusCode()).isEqualTo(200);assertThat(login.body()).contains("name=\"password\"").doesNotContain("Continuar con Google","oauth2/authorization");
   assertThat(b.get("/register").body()).doesNotContain("Continuar con Google");
   assertThat(b.get("/oauth2/authorization/google").statusCode()).isNotEqualTo(302);
  }
 }
}
