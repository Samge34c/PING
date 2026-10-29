package com.ping.auth;
import java.time.Duration;
import org.junit.jupiter.api.Test;

import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import static org.assertj.core.api.Assertions.*;
/** Account e-mail content (DEC-062): same action in HTML and text, real lifetimes, escaping and e-mail-safe markup. */
class AccountMailTests {
 static final String BASE="https://ping.example.test",TOKEN="EJEMPLO_NO_VALIDO-0123456789abcdefghijklmnopqrst";
 static AccountMail mail(){var r=new ClassLoaderTemplateResolver();r.setPrefix("templates/");r.setSuffix(".html");r.setTemplateMode(TemplateMode.HTML);r.setCharacterEncoding("UTF-8");var e=new org.thymeleaf.spring6.SpringTemplateEngine();e.setTemplateResolver(r);return new AccountMail(e);}
 @Test void everyKindHasItsSubjectButtonDestinationAndLifetimeInBothParts(){
  record Case(String label,String subject,String action,String path,String purpose,String expiry){}
  for(var c:new Case[]{new Case("VERIFY","Verifica tu correo de PING","Verificar mi correo","/account/verify","VERIFY","24 horas"),new Case("ACTIVATE","Activa tu cuenta de PING","Activar mi cuenta","/account/reset","RESET","30 minutos"),new Case("RESET","Recupera tu acceso a PING","Elegir nueva contraseña","/account/reset","RESET","30 minutos"),new Case("EXISTS","Ya tienes una cuenta de PING","Elegir nueva contraseña","/account/reset","RESET","30 minutos")}){
   var m=mail().render(AccountMail.Kind.of(c.label()),BASE,TOKEN,AccountService.lifetime(c.purpose()));String url=BASE+c.path()+"?token="+TOKEN;
   assertThat(m.subject()).isEqualTo(c.subject());
   // The designed button is a real link to the exact destination; the long URL stays only as the discreet alternative.
   assertThat(m.html()).contains("class=\"ping-button\" href=\""+url+"\"",">"+c.action()+"</a>","El enlace caduca en "+c.expiry()+" y solo funciona una vez.").containsOnlyOnce(">"+url+"</a>");
   assertThat(m.text()).contains(c.action()+":\n"+url,"El enlace caduca en "+c.expiry());
  }
 }
 @Test void markupIsEmailSafeWithLogoFallbackAndNoScriptsFormsOrExternalCss(){
  var html=mail().render(AccountMail.Kind.RESET,BASE,TOKEN,Duration.ofMinutes(30)).html();
  assertThat(html).contains("src=\""+BASE+"/img/email/ping-logo.png\"","width=\"88\"","height=\"32\"","alt=\"PING\"","max-width:600px","font-size:16px","role=\"presentation\"","-apple-system")
   .doesNotContain("<script","<form","<link","data:image","<svg","utm_");
 }
 @Test void dynamicValuesAreEscaped(){
  var html=mail().render(AccountMail.Kind.VERIFY,BASE,"\"><b>x</b>&",Duration.ofHours(24)).html();
  assertThat(html).doesNotContain("<b>x</b>").contains("&lt;b&gt;x&lt;/b&gt;&amp;");
 }
 @Test void lifetimesComeFromTheTokenConfiguration(){
  assertThat(AccountService.lifetime("RESET")).isEqualTo(Duration.ofMinutes(30));assertThat(AccountService.lifetime("VERIFY")).isEqualTo(Duration.ofHours(24));
  assertThat(AccountMail.lifetime(Duration.ofHours(1))).isEqualTo("1 hora");assertThat(AccountMail.lifetime(Duration.ofMinutes(90))).isEqualTo("90 minutos");
 }
}
