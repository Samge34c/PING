package com.ping.auth;
import java.io.IOException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
@Component @Profile("!bootstrap")
public class LoginSuccess implements AuthenticationSuccessHandler {
 private final AccountService accounts;private final AuthSupport support;
 public LoginSuccess(AccountService a,AuthSupport s){accounts=a;support=s;}
 @Override public void onAuthenticationSuccess(HttpServletRequest r,HttpServletResponse response,Authentication auth)throws IOException,ServletException{
  var session=r.getSession();Long link=null;String provider=null;
  try{
   PingUser user;
   if(auth instanceof OAuth2AuthenticationToken oauth){
    var external=(OAuth2User)oauth.getPrincipal();provider=oauth.getAuthorizedClientRegistrationId();Object linking=session.getAttribute("pingLinkAccount");Object until=session.getAttribute("pingLinkUntil");Object epoch=session.getAttribute("pingLinkEpoch");session.removeAttribute("pingLinkAccount");session.removeAttribute("pingLinkUntil");session.removeAttribute("pingLinkEpoch");
    // Linking continues the session that started it; a password reset meanwhile (new epoch) ends it instead.
    if(linking!=null&&(!(until instanceof Long expiry)||expiry<=System.currentTimeMillis()||!(linking instanceof Long owner)||!(epoch instanceof Long started)||accounts.user(owner).epoch()!=started))throw new AccountService.ExternalAccessException("expired","Vuelve a iniciar la vinculación desde Mi cuenta.");link=linking instanceof Long id?id:null;
    // Identity is provider+stable subject; Google name/picture are never copied into PING.
    String subject=external.getAttribute(provider.equals("google")?"sub":"id");if(subject==null)subject=external.getName();long id=accounts.external(provider,subject,external.getAttribute("email"),provider.equals("google")&&Boolean.TRUE.equals(external.getAttribute("email_verified")),link);user=accounts.user(id);
    if(!user.verified()){SecurityContextHolder.clearContext();session.removeAttribute("SPRING_SECURITY_CONTEXT");response.sendRedirect("/login?sent=true");return;}
    authenticate(user,r,response);session.removeAttribute("pingLinkOffer");
   }else user=(PingUser)auth.getPrincipal();
   Object previous=session.getAttribute("pingDemoParticipantId");if(previous instanceof Long id&&accounts.unlinked(id)&&"participant".equals(session.getAttribute("pingEntryMode")))session.setAttribute("pingLegacyOwnedParticipant",id);
   session.removeAttribute("pingDemoParticipantId");session.removeAttribute("pingEntryMode");
   String destination=support.next((String)session.getAttribute("pingReturnTo"));
   var selectedEvent=com.ping.entry.ParticipantEntryController.pendingEventId(session);
   if(link!=null&&previous instanceof Long id&&accounts.owns(user.id(),id)) {
    support.entered(session,id);
   } else if(destination.equals("/join")&&selectedEvent.isPresent()) {
    accounts.participant(user.id(),selectedEvent.get()).ifPresent(id->support.entered(session,id));
   } else {
    var existing=accounts.participations(user.id());if(!existing.isEmpty())support.entered(session,existing.getFirst().participantId());
   }
   session.removeAttribute("pingReturnTo");
   if(link!=null)destination="/account?linked="+provider;
   else if(session.getAttribute("pingLinkOffer") instanceof String offer){session.removeAttribute("pingLinkOffer");destination="/account?offer="+offer;}
   if(session.getAttribute("pingLegacyOwnedParticipant")!=null)destination="/account/link-session";
   accounts.audit(user.id(),"LOGIN",user.id(),null);response.sendRedirect(destination);
  }catch(com.ping.web.BusinessRuleException error){
   String reason=error instanceof AccountService.ExternalAccessException x?x.reason:"provider";String name="facebook".equals(provider)?"facebook":"google";
   SecurityContextHolder.clearContext();session.removeAttribute("SPRING_SECURITY_CONTEXT");
   if(link!=null){authenticate(accounts.user(link),r,response);response.sendRedirect("/account?linkError="+reason+"&provider="+name);return;}
   // Same e-mail as an unlinked PING account: prove access with that account first, then offer linking.
   if(reason.equals("existing"))session.setAttribute("pingLinkOffer",name);
   response.sendRedirect("/login?oauthError="+reason+"&provider="+name);
  }
 }
 private void authenticate(PingUser user,HttpServletRequest r,HttpServletResponse response){var context=SecurityContextHolder.createEmptyContext();context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user,null,user.getAuthorities()));SecurityContextHolder.setContext(context);new HttpSessionSecurityContextRepository().saveContext(context,r,response);}
}
