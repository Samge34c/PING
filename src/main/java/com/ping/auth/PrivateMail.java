package com.ping.auth;
import java.net.URI;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.mail.*;
import com.ping.web.BusinessRuleException;
@Service @Profile("!bootstrap")
public class PrivateMail {
 // SMTP (JavaMailSender) en local; HttpApiMail por HTTPS donde el proveedor bloquea SMTP.
 private final ObjectProvider<MailSender> sender;private final Environment env;
 public PrivateMail(ObjectProvider<MailSender> s,Environment e){sender=s;env=e;}
 public boolean available(){return sender.getIfAvailable()!=null&&!env.getProperty("ping.public-base-url","").isBlank()&&!env.getProperty("ping.mail.from","").isBlank();}
 public void send(String email,String purpose,String token){
  if(!available())throw new BusinessRuleException("El correo todavía no está configurado. Contacta con la organización.");
  String base=env.getProperty("ping.public-base-url","").replaceAll("/+$","");URI uri=URI.create(base);
  if(!SetHolder.SCHEMES.contains(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null)throw new IllegalStateException("PING_PUBLIC_BASE_URL must be an absolute HTTP(S) origin");
  var msg=new SimpleMailMessage();msg.setFrom(env.getProperty("ping.mail.from"));msg.setTo(email);msg.setSubject(purpose.equals("VERIFY")?"Verifica tu cuenta de PING":"Recupera tu acceso a PING");msg.setText((purpose.equals("VERIFY")?"Confirma tu correo":"Elige una nueva contraseña")+". Enlace personal de un solo uso:\n"+base+(purpose.equals("VERIFY")?"/account/verify":"/account/reset")+"?token="+token+"\nSi no lo solicitaste, ignora este mensaje.");
  try{sender.getObject().send(msg);}catch(MailException ex){throw new BusinessRuleException("No pudimos enviar el correo ahora. Vuelve a intentarlo más tarde.");}
 }
 private static class SetHolder {static final java.util.Set<String> SCHEMES=java.util.Set.of("http","https");}
}
