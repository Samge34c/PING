package com.ping.auth;
import java.util.*;
import java.util.regex.*;
import jakarta.servlet.http.*;
import org.springframework.context.annotation.*;
import org.springframework.web.servlet.*;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
@Configuration @Profile("!bootstrap")
public class AccessPolicy implements WebMvcConfigurer {
 private final AuthSupport auth;private final AccountService accounts;private final JdbcTemplate jdbc;
 public AccessPolicy(AuthSupport a,AccountService s,JdbcTemplate j){auth=a;accounts=s;jdbc=j;}
 private void deny(){throw new AccessDeniedException("No tienes permiso para esta acción.");}
 private long eventOf(String table,long id){var rows=jdbc.query("select event_id from "+table+" where id=?",(r,n)->r.getLong(1),id);if(rows.isEmpty())deny();return rows.getFirst();}
 @Override public void addInterceptors(InterceptorRegistry registry){registry.addInterceptor(new HandlerInterceptor(){@Override public boolean preHandle(HttpServletRequest r,HttpServletResponse response,Object handler)throws Exception{
  if(auth.legacyFixture())return true;
  String path=org.springframework.web.util.UrlPathHelper.defaultInstance.getLookupPathForRequest(r);var current=auth.current();if(current==null)return true;
  var stored=SecurityContextHolder.getContext().getAuthentication().getPrincipal();if(stored instanceof PingUser old&&old.epoch()!=current.epoch()){r.getSession().invalidate();SecurityContextHolder.clearContext();if(path.equals("/me/state")){response.setStatus(401);response.setContentType("application/json");response.getWriter().write("{\"error\":\"SESSION_REQUIRED\",\"entryUrl\":\"/login\"}");}else response.sendRedirect("/login?expired=true");return false;}
  var s=r.getSession();boolean demo=auth.demo()&&"demo".equals(s.getAttribute("pingEntryMode"));Object raw=s.getAttribute("pingDemoParticipantId");Long self=raw instanceof Long id?id:null;
  if(path.equals("/organizer")||path.startsWith("/organizer/redemptions")||path.equals("/admin")||path.equals("/events")||path.equals("/participants")){if(!current.general()&&accounts.managed(current.id()).isEmpty())deny();if(path.equals("/admin")&&r.getParameter("eventId")!=null)auth.manage(Long.parseLong(r.getParameter("eventId")));if(path.equals("/participants")&&!r.getMethod().equals("GET")&&!auth.demo())deny();}
  if(path.equals("/events/new")||(path.equals("/events")&&!r.getMethod().equals("GET"))){if(!current.general())deny();}
  if(path.startsWith("/organizer/access")||path.startsWith("/interests/categories")||path.startsWith("/interests/taxonomy")){if(!current.general())deny();}
  if(path.startsWith("/demo")&&!path.equals("/demo/exit")){if(!auth.demo())deny();if(path.equals("/demo")&&r.getMethod().equals("POST")){String id=r.getParameter("participantId");if(id==null||!id.matches("[0-9]+")||jdbc.queryForObject("select count(*) from participants p join demo_scenarios d on d.event_id=p.event_id left join account_participations a on a.participant_id=p.id where p.id=? and a.participant_id is null",Integer.class,Long.parseLong(id))==0)deny();}}
  if(path.equals("/participants/new")&&!auth.demo())deny();
  Matcher event=Pattern.compile("^/events/([0-9]+)(/.*)?$").matcher(path);
  if(event.matches()){long id=Long.parseLong(event.group(1));String tail=Objects.toString(event.group(2),"");if((Set.of("/hub","/map","/plan").contains(tail)||tail.matches("/floorplan/images/[a-fA-F0-9-]{36}"))){if(!auth.canManage(id)&&(self==null||!accounts.owns(current.id(),self)||eventOf("participants",self)!=id)&&!demo)deny();}else auth.manage(id);}
  Matcher participant=Pattern.compile("^/participants/([0-9]+)(/.*)?$").matcher(path);
  if(participant.matches()){long id=Long.parseLong(participant.group(1));boolean own=accounts.owns(current.id(),id);if(!own&&!demo){if(!r.getMethod().equals("GET")||Objects.toString(participant.group(2),"").equals("/interests"))deny();auth.manage(eventOf("participants",id));}}
  Matcher claim=Pattern.compile("^/admin/claims/([0-9]+)$").matcher(path);if(claim.matches()){var ids=jdbc.query("select p.event_id from reward_claims c join participants p on p.id=c.participant_id where c.id=?",(rows,n)->rows.getLong(1),Long.parseLong(claim.group(1)));if(ids.isEmpty())deny();auth.manage(ids.getFirst());}
  if(path.startsWith("/me")||path.startsWith("/matches")||path.equals("/connect")){if(self==null){if(path.equals("/me/state")){response.setStatus(401);response.setContentType("application/json");response.getWriter().write("{\"error\":\"SESSION_REQUIRED\",\"entryUrl\":\"/join\"}");}else response.sendRedirect("/join");return false;}if(!accounts.owns(current.id(),self)&&!demo)deny();}
  return true;
 }});}
}
