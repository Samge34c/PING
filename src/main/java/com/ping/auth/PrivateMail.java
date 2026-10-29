package com.ping.auth;
import java.net.URI;
import java.time.Duration;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.mail.*;
import org.springframework.mail.javamail.*;
import com.ping.web.BusinessRuleException;
@Service @Profile("!bootstrap")
public class PrivateMail {
 // SMTP (JavaMailSender) en local; HttpApiMail por HTTPS donde el proveedor bloquea SMTP.
 private final ObjectProvider<MailSender> sender;private final Environment env;private final AccountMail templates;
 public PrivateMail(ObjectProvider<MailSender> s,Environment e,AccountMail t){sender=s;env=e;templates=t;}
 /** Text message that also carries the designed HTML; HTTP API adapters send both parts. */
 public static class HtmlMailMessage extends SimpleMailMessage{private final String html;public HtmlMailMessage(String html){this.html=html;}public String getHtml(){return html;}}
 public boolean available(){return sender.getIfAvailable()!=null&&!env.getProperty("ping.public-base-url","").isBlank()&&!env.getProperty("ping.mail.from","").isBlank();}
 public void send(String email,String purpose,String token,Duration ttl){
  if(!available())throw new BusinessRuleException("El correo todavía no está configurado. Contacta con la organización.");
  String base=env.getProperty("ping.public-base-url","").replaceAll("/+$","");URI uri=URI.create(base);
  if(!SetHolder.SCHEMES.contains(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null)throw new IllegalStateException("PING_PUBLIC_BASE_URL must be an absolute HTTP(S) origin");
  var content=templates.render(AccountMail.Kind.of(purpose),base,token,ttl);String from=env.getProperty("ping.mail.from");
  try{
   var transport=sender.getObject();
   if(transport instanceof JavaMailSender smtp){
    // SMTP (real server or the loopback development mailbox): multipart/alternative with the same action in both parts.
    var mime=smtp.createMimeMessage();var helper=new MimeMessageHelper(mime,true,"UTF-8");helper.setFrom(from);helper.setTo(email);helper.setSubject(content.subject());helper.setText(content.text(),content.html());smtp.send(mime);
   }else{var msg=new HtmlMailMessage(content.html());msg.setFrom(from);msg.setTo(email);msg.setSubject(content.subject());msg.setText(content.text());transport.send(msg);}
  }catch(MailException|jakarta.mail.MessagingException ex){throw new BusinessRuleException("No pudimos enviar el correo ahora. Vuelve a intentarlo más tarde.");}
 }
 private static class SetHolder {static final java.util.Set<String> SCHEMES=java.util.Set.of("http","https");}
}
