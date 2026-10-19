package com.ping.auth;
import java.util.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.core.context.SecurityContextHolder;
@Component @Profile("!bootstrap")
public class AuthSupport{
 private final AccountService accounts;private final Environment env;
 public AuthSupport(AccountService a,Environment e){accounts=a;env=e;}
 public boolean legacyFixture(){return env.matchesProfiles("test")&&getClass().getClassLoader().getResource("com/ping/LegacySecurityFixture.class")!=null;}
 public PingUser current(){var a=SecurityContextHolder.getContext().getAuthentication();return a!=null&&a.getPrincipal() instanceof PingUser p?accounts.user(p.id()):null;}
 public PingUser require(){var a=current();if(a==null)throw new org.springframework.security.access.AccessDeniedException("Entra a tu cuenta de PING.");return a;}
 public boolean demo(){return env.getProperty("ping.demo.enabled",Boolean.class,false)&&current()!=null&&current().general();}
 public boolean canManage(long event){var a=current();return a!=null&&(a.general()||accounts.managed(a.id()).contains(event));}
 public void manage(long event){if(!canManage(event))throw new org.springframework.security.access.AccessDeniedException("No tienes permiso para administrar este evento.");}
 public String next(String value){return value!=null&&Set.of("/join","/me","/connect","/organizer","/account").contains(value)?value:"/me";}
 public Optional<Long> existing(long event){var a=current();return a==null?Optional.empty():accounts.participant(a.id(),event);}
 public boolean owned(long id){var a=current();return a!=null&&accounts.owns(a.id(),id);}
 public void entered(HttpSession s,long participant){s.setAttribute("pingDemoParticipantId",participant);s.setAttribute("pingEntryMode","participant");}
}
