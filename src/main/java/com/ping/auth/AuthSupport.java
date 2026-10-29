package com.ping.auth;
import java.util.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
@Component @Profile("!bootstrap")
public class AuthSupport{
 private static final String CURRENT_ACCOUNT=AuthSupport.class.getName()+".currentAccount";
 private record CurrentAccount(Authentication authentication,HttpSession session,String sessionId,PingUser user){}
 private final AccountService accounts;private final Environment env;
 public AuthSupport(AccountService a,Environment e){accounts=a;env=e;}
 public boolean legacyFixture(){return env.matchesProfiles("test")&&getClass().getClassLoader().getResource("com/ping/LegacySecurityFixture.class")!=null;}
 public PingUser current(){
  var authentication=SecurityContextHolder.getContext().getAuthentication();
  var request=RequestContextHolder.getRequestAttributes();
  if(authentication==null||!(authentication.getPrincipal() instanceof PingUser principal)){
   if(request!=null)request.removeAttribute(CURRENT_ACCOUNT,RequestAttributes.SCOPE_REQUEST);
   return null;
  }
  // Reuse a fresh read only for this request and this exact authentication. Never
  // retain roles in the session: the next request must observe resets/revocations.
  var session=request instanceof org.springframework.web.context.request.ServletRequestAttributes servlet?servlet.getRequest().getSession(false):null;
  var sessionId=session==null?null:session.getId();
  if(request!=null&&request.getAttribute(CURRENT_ACCOUNT,RequestAttributes.SCOPE_REQUEST) instanceof CurrentAccount resolved
    &&resolved.authentication()==authentication&&resolved.session()==session&&Objects.equals(resolved.sessionId(),sessionId))return resolved.user();
  var user=accounts.user(principal.id());
  if(request!=null)request.setAttribute(CURRENT_ACCOUNT,new CurrentAccount(authentication,session,sessionId,user),RequestAttributes.SCOPE_REQUEST);
  return user;
 }
 public PingUser require(){var a=current();if(a==null)throw new org.springframework.security.access.AccessDeniedException("Entra a tu cuenta de PING.");return a;}
 public boolean demo(){if(!env.getProperty("ping.demo.enabled",Boolean.class,false))return false;var user=current();return user!=null&&user.general();}
 public boolean canManage(long event){var a=current();return a!=null&&(a.general()||accounts.managed(a.id()).contains(event));}
 public void manage(long event){if(!canManage(event))throw new org.springframework.security.access.AccessDeniedException("No tienes permiso para administrar este evento.");}
 public String next(String value){return value!=null&&Set.of("/join","/me","/connect","/organizer","/account").contains(value)?value:"/me";}
 public Optional<Long> existing(long event){var a=current();return a==null?Optional.empty():accounts.participant(a.id(),event);}
 public boolean owned(long id){var a=current();return a!=null&&accounts.owns(a.id(),id);}
 public void entered(HttpSession s,long participant){s.setAttribute("pingDemoParticipantId",participant);s.setAttribute("pingEntryMode","participant");}
}
