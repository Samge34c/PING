package com.ping.auth;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.mail.*;
import org.springframework.stereotype.Component;
/** Transactional mail over an HTTPS API for hosts that block SMTP (Render Free). Only Brevo is implemented; sends htmlContent plus textContent.
 *  The API key travels only in the request header; neither the key nor message bodies (with tokens) are logged. */
@Component @Primary @Profile("!bootstrap") @ConditionalOnProperty(name="ping.mail.api.provider",havingValue="brevo")
public class HttpApiMail implements MailSender {
 private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build();
 private final URI endpoint;private final String key;private final String senderName;
 public HttpApiMail(@Value("${ping.mail.api.endpoint:https://api.brevo.com/v3/smtp/email}") String endpoint,
                    @Value("${ping.mail.api.key:}") String key,@Value("${ping.mail.from-name:PING}") String senderName){
  this.endpoint=URI.create(endpoint);this.key=key;this.senderName=senderName;
  if(key.isBlank())throw new IllegalStateException("PING_MAIL_API_KEY is required when ping.mail.api.provider=brevo");
  if(!"https".equals(this.endpoint.getScheme())&&!"127.0.0.1".equals(this.endpoint.getHost()))throw new IllegalStateException("The mail API endpoint must use HTTPS");
 }
 @Override public void send(SimpleMailMessage message){
  if(message.getFrom()==null||message.getTo()==null||message.getTo().length!=1)throw new MailPreparationException("Exactly one sender and one recipient are required");
  String body="{\"sender\":{\"name\":"+json(senderName)+",\"email\":"+json(message.getFrom())+"},\"to\":[{\"email\":"+json(message.getTo()[0])+"}],"
   +"\"subject\":"+json(Objects.toString(message.getSubject(),""))+(message instanceof PrivateMail.HtmlMailMessage h&&h.getHtml()!=null?",\"htmlContent\":"+json(h.getHtml()):"")+",\"textContent\":"+json(Objects.toString(message.getText(),""))+"}";
  var request=HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(15)).header("api-key",key).header("accept","application/json")
   .header("content-type","application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
  try{
   int status=client.send(request,HttpResponse.BodyHandlers.discarding()).statusCode();
   if(status/100!=2)throw new MailSendException("Mail API rejected the message with HTTP "+status);
  }catch(java.io.IOException error){throw new MailSendException("Mail API unavailable",error);}
  catch(InterruptedException error){Thread.currentThread().interrupt();throw new MailSendException("Mail API interrupted",error);}
 }
 @Override public void send(SimpleMailMessage... messages){for(var m:messages)send(m);}
 static String json(String value){
  var out=new StringBuilder("\"");
  for(char c:value.toCharArray()){switch(c){case '"'->out.append("\\\"");case '\\'->out.append("\\\\");case '\n'->out.append("\\n");case '\r'->out.append("\\r");case '\t'->out.append("\\t");
   default->{if(c<0x20)out.append(String.format("\\u%04x",(int)c));else out.append(c);}}}
  return out.append('"').toString();
 }
}
