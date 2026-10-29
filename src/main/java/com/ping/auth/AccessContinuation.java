package com.ping.auth;
import java.time.Instant;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import com.ping.entry.ParticipantEntryController;
/** Single post-access policy shared by password, Google, ownership confirmation and e-mail verification. */
@Component @Profile("!bootstrap")
public class AccessContinuation {
 /** E-mail typed in the access forms, kept server-side to prefill the next form; never a password. */
 public static final String EMAIL="pingAccessEmail",PENDING_LINK="pingPendingLink";
 /** Google identity waiting for proof of ownership of the PING account with the same verified e-mail. Session-bound, short-lived. */
 public record PendingLink(String provider,String subject,String email,long account,Instant expires) implements java.io.Serializable{
  public static PendingLink of(String provider,String subject,String email,long account){return new PendingLink(provider,subject,email,account,Instant.now().plusSeconds(600));}
  public boolean expired(){return !Instant.now().isBefore(expires);}
 }
 private final AccountService accounts;private final AuthSupport support;
 public AccessContinuation(AccountService a,AuthSupport s){accounts=a;support=s;}
 public static PendingLink pending(HttpSession s){return s.getAttribute(PENDING_LINK) instanceof PendingLink p&&!p.expired()?p:null;}
 /** Store the authenticated account in the session (callers rotate the session id before). */
 public void authenticate(PingUser user,HttpServletRequest r,HttpServletResponse response){var context=SecurityContextHolder.createEmptyContext();context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user,null,user.getAuthorities()));SecurityContextHolder.setContext(context);new HttpSessionSecurityContextRepository().saveContext(context,r,response);}
 /** Sign in where the person has just proven their identity (verification link, ownership check): new session id, then continue. */
 public String signIn(PingUser user,HttpServletRequest r,HttpServletResponse response){r.getSession();r.changeSessionId();authenticate(user,r,response);return next(user,r.getSession());}
 /** Where to go after access: the chosen event first, then the account's own space; never another event's participation. */
 public String next(PingUser user,HttpSession session){
  var link=session.getAttribute(PENDING_LINK) instanceof PendingLink p?p:null;
  if(link!=null&&link.account()==user.id()&&!link.expired()){accounts.audit(user.id(),"LOGIN",user.id(),null);return "/login/link";}
  session.removeAttribute(PENDING_LINK);session.removeAttribute(EMAIL);
  Object previous=session.getAttribute("pingDemoParticipantId");if(previous instanceof Long id&&accounts.unlinked(id)&&"participant".equals(session.getAttribute("pingEntryMode")))session.setAttribute("pingLegacyOwnedParticipant",id);
  session.removeAttribute("pingDemoParticipantId");session.removeAttribute("pingEntryMode");
  String requested=support.next((String)session.getAttribute("pingReturnTo"));session.removeAttribute("pingReturnTo");
  var event=ParticipantEntryController.pendingEventId(session);String destination;
  if(event.isPresent()&&(requested.equals("/join")||requested.equals("/me"))){
   // Event chosen by code/QR: its own participation, or the entry form to complete only what is missing.
   var own=accounts.participant(user.id(),event.get());own.ifPresent(id->support.entered(session,id));destination=own.isPresent()?"/me":"/join";
  }else{
   var existing=accounts.participations(user.id());if(!existing.isEmpty())support.entered(session,existing.getFirst().participantId());
   destination=requested.equals("/me")&&existing.isEmpty()?"/join":requested;
  }
  if(session.getAttribute("pingLegacyOwnedParticipant")!=null)destination="/account/link-session";
  accounts.audit(user.id(),"LOGIN",user.id(),null);return destination;
 }
}
