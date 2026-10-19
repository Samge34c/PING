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
  var session=r.getSession();
  try{
   PingUser user;
   if(auth instanceof OAuth2AuthenticationToken oauth){
    var external=(OAuth2User)oauth.getPrincipal();String provider=oauth.getAuthorizedClientRegistrationId();Object linking=session.getAttribute("pingLinkAccount");Object until=session.getAttribute("pingLinkUntil");session.removeAttribute("pingLinkAccount");session.removeAttribute("pingLinkUntil");if(linking!=null&&(!(until instanceof Long expiry)||expiry<=System.currentTimeMillis()))throw new com.ping.web.BusinessRuleException("Vuelve a iniciar la vinculación desde Mi cuenta.");Long link=linking instanceof Long id?id:null;
    String subject=external.getAttribute(provider.equals("google")?"sub":"id");if(subject==null)subject=external.getName();long id=accounts.external(provider,subject,external.getAttribute("email"),provider.equals("google")&&Boolean.TRUE.equals(external.getAttribute("email_verified")),link);user=accounts.user(id);
    if(!user.verified()){SecurityContextHolder.clearContext();session.removeAttribute("SPRING_SECURITY_CONTEXT");response.sendRedirect("/login?sent=true");return;}
    var replacement=UsernamePasswordAuthenticationToken.authenticated(user,null,user.getAuthorities());SecurityContextHolder.getContext().setAuthentication(replacement);new HttpSessionSecurityContextRepository().saveContext(SecurityContextHolder.getContext(),r,response);
   }else user=(PingUser)auth.getPrincipal();
   Object previous=session.getAttribute("pingDemoParticipantId");if(previous instanceof Long id&&accounts.unlinked(id)&&"participant".equals(session.getAttribute("pingEntryMode")))session.setAttribute("pingLegacyOwnedParticipant",id);
   session.removeAttribute("pingDemoParticipantId");session.removeAttribute("pingEntryMode");
   var existing=accounts.participations(user.id());if(!existing.isEmpty())support.entered(session,existing.getFirst().participantId());String destination=support.next((String)session.getAttribute("pingReturnTo"));session.removeAttribute("pingReturnTo");
   if(session.getAttribute("pingLegacyOwnedParticipant")!=null)destination="/account/link-session";
   accounts.audit(user.id(),"LOGIN",user.id(),null);response.sendRedirect(destination);
  }catch(com.ping.web.BusinessRuleException error){SecurityContextHolder.clearContext();session.removeAttribute("SPRING_SECURITY_CONTEXT");response.sendRedirect("/login?oauthError=true");}
 }
}
