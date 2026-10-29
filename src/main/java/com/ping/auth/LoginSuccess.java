package com.ping.auth;
import java.io.IOException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
@Component @Profile("!bootstrap")
public class LoginSuccess implements AuthenticationSuccessHandler {
 private final AccountService accounts;private final AuthSupport support;private final AccessContinuation continuation;
 public LoginSuccess(AccountService a,AuthSupport s,AccessContinuation c){accounts=a;support=s;continuation=c;}
 @Override public void onAuthenticationSuccess(HttpServletRequest r,HttpServletResponse response,Authentication auth)throws IOException,ServletException{
  var session=r.getSession();Long link=null;String provider=null,subject=null,email=null;boolean verified=false;
  try{
   if(!(auth instanceof OAuth2AuthenticationToken oauth)){response.sendRedirect(continuation.next((PingUser)auth.getPrincipal(),session));return;}
   var external=(OAuth2User)oauth.getPrincipal();provider=oauth.getAuthorizedClientRegistrationId();Object linking=session.getAttribute("pingLinkAccount");Object until=session.getAttribute("pingLinkUntil");Object epoch=session.getAttribute("pingLinkEpoch");session.removeAttribute("pingLinkAccount");session.removeAttribute("pingLinkUntil");session.removeAttribute("pingLinkEpoch");
   // Linking from Mi cuenta continues the session that started it; a password reset meanwhile (new epoch) ends it instead.
   if(linking!=null&&(!(until instanceof Long expiry)||expiry<=System.currentTimeMillis()||!(linking instanceof Long owner)||!(epoch instanceof Long started)||accounts.user(owner).epoch()!=started))throw new AccountService.ExternalAccessException("expired","Vuelve a iniciar la vinculación desde Mi cuenta.");link=linking instanceof Long id?id:null;
   // Identity is provider+stable subject; Google name/picture are never copied into PING.
   subject=external.getAttribute(provider.equals("google")?"sub":"id");if(subject==null)subject=external.getName();email=external.getAttribute("email");verified=provider.equals("google")&&Boolean.TRUE.equals(external.getAttribute("email_verified"));
   PingUser user=accounts.user(accounts.external(provider,subject,email,verified,link));
   if(!user.verified()){SecurityContextHolder.clearContext();session.removeAttribute("SPRING_SECURITY_CONTEXT");response.sendRedirect("/login?sent=true");return;}
   continuation.authenticate(user,r,response);
   if(link==null){response.sendRedirect(continuation.next(user,session));return;}
   Object previous=session.getAttribute("pingDemoParticipantId");if(previous instanceof Long id&&accounts.owns(user.id(),id))support.entered(session,id);
   accounts.audit(user.id(),"LOGIN",user.id(),null);response.sendRedirect("/account?linked="+provider);
  }catch(com.ping.web.BusinessRuleException error){
   String reason=error instanceof AccountService.ExternalAccessException x?x.reason:"provider";String name="facebook".equals(provider)?"facebook":"google";
   SecurityContextHolder.clearContext();session.removeAttribute("SPRING_SECURITY_CONTEXT");
   if(link!=null){continuation.authenticate(accounts.user(link),r,response);response.sendRedirect("/account?linkError="+reason+"&provider="+name);return;}
   // Same verified e-mail as an unlinked PING account: keep this identity server-side and ask for proof of ownership in one step.
   if(reason.equals("existing")&&verified){var owner=accounts.find(AccountService.email(email));if(owner.isPresent()){session.setAttribute(AccessContinuation.PENDING_LINK,AccessContinuation.PendingLink.of(name,subject,AccountService.email(email),owner.get()));response.sendRedirect("/login/link");return;}}
   response.sendRedirect("/login?oauthError="+reason+"&provider="+name);
  }
 }
}
