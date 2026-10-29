package com.ping.support;
import java.util.*;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
/** Captures account mail sent through a mocked SMTP transport as subject + plain-text + HTML parts. */
public final class MailCapture {
 public record Mail(String to,String subject,String text,String html){public String getSubject(){return subject;}public String getText(){return text;}}
 private MailCapture(){}
 public static void capture(JavaMailSender sender,List<Mail> into){
  when(sender.createMimeMessage()).thenAnswer(i->new MimeMessage(Session.getInstance(new Properties())));
  doAnswer(i->{into.add(read(i.getArgument(0)));return null;}).when(sender).send(any(MimeMessage.class));
 }
 static Mail read(MimeMessage m)throws Exception{m.saveChanges();var parts=new HashMap<String,String>();collect(m,parts);return new Mail(m.getRecipients(Message.RecipientType.TO)[0].toString(),m.getSubject(),parts.get("text/plain"),parts.get("text/html"));}
 static void collect(Part p,Map<String,String> parts)throws Exception{
  if(p.isMimeType("multipart/*")){var mp=(Multipart)p.getContent();for(int i=0;i<mp.getCount();i++)collect(mp.getBodyPart(i),parts);}
  else if(p.isMimeType("text/plain"))parts.putIfAbsent("text/plain",(String)p.getContent());
  else if(p.isMimeType("text/html"))parts.putIfAbsent("text/html",(String)p.getContent());
 }
}
