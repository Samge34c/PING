package com.ping.auth;
import java.time.Duration;
import org.springframework.stereotype.Component;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;
/** Account e-mails (DEC-062): one shared design (templates/mail/account.html) plus a plain-text alternative with the same action. */
@Component @org.springframework.context.annotation.Profile("!bootstrap")
public class AccountMail {
 /** Message kinds used by AccountService labels; the action path is the real flow, never invented. */
 public enum Kind {
  VERIFY("Verifica tu correo de PING","Verifica tu correo","Confirma que esta dirección es tuya para activar tu cuenta de PING. Te pediremos la contraseña que elegiste al registrarte.","Verificar mi correo","¿No creaste una cuenta en PING? Ignora este mensaje: la cuenta no se activará.","/account/verify"),
  ACTIVATE("Activa tu cuenta de PING","Activa tu cuenta","Elige una contraseña para activar tu cuenta de PING. Si alguien usó otra contraseña con este correo, dejará de funcionar.","Activar mi cuenta","¿No lo pediste? Ignora este mensaje: la cuenta seguirá sin activar.","/account/reset"),
  RESET("Recupera tu acceso a PING","Elige una nueva contraseña","Pediste recuperar tu acceso a PING. Al guardar la contraseña nueva se cerrarán las sesiones abiertas.","Elegir nueva contraseña","¿No lo pediste? Ignora este mensaje: tu contraseña no cambia.","/account/reset"),
  EXISTS("Ya tienes una cuenta de PING","Ya tienes una cuenta","Alguien intentó crear una cuenta de PING con este correo. Si fuiste tú, entra con tu contraseña o elige una nueva.","Elegir nueva contraseña","¿No fuiste tú? Ignora este mensaje: tu cuenta no cambia.","/account/reset");
  public final String subject,title,body,action,ignore,path;
  Kind(String subject,String title,String body,String action,String ignore,String path){this.subject=subject;this.title=title;this.body=body;this.action=action;this.ignore=ignore;this.path=path;}
  public static Kind of(String label){return switch(label){case "VERIFY"->VERIFY;case "ACTIVATE"->ACTIVATE;case "EXISTS"->EXISTS;default->RESET;};}
 }
 public record Content(String subject,String text,String html){}
 private final ITemplateEngine templates;
 public AccountMail(ITemplateEngine templates){this.templates=templates;}
 /** Human expiry from the configured token lifetime, e.g. "24 horas" or "30 minutos". */
 static String lifetime(Duration ttl){long m=ttl.toMinutes();if(m%60==0){long h=m/60;return h+(h==1?" hora":" horas");}return m+(m==1?" minuto":" minutos");}
 public Content render(Kind kind,String base,String token,Duration ttl){
  String url=base+kind.path+"?token="+token;String expiry="El enlace caduca en "+lifetime(ttl)+" y solo funciona una vez.";
  var c=new Context(java.util.Locale.forLanguageTag("es"));
  c.setVariable("kind",kind);c.setVariable("url",url);c.setVariable("expiry",expiry);c.setVariable("logo",base+"/img/email/ping-logo.png");
  String text=kind.title+"\n\n"+kind.body+"\n\n"+kind.action+":\n"+url+"\n\n"+expiry+"\n\n"+kind.ignore+"\n\n--\nPING · Conexiones en tu evento\nRecibes este correo por una acción en tu cuenta de PING.\n";
  return new Content(kind.subject,text,templates.process("mail/account",c));
 }
}
