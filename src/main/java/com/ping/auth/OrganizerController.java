package com.ping.auth;
import org.springframework.stereotype.Controller;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import com.ping.event.EventService;
@Controller @Profile("!bootstrap")
public class OrganizerController {
 private final AuthSupport auth;private final AccountService accounts;private final EventService events;
 public OrganizerController(AuthSupport a,AccountService s,EventService e){auth=a;accounts=s;events=e;}
 @GetMapping("/organizer") public String index(Model m){var user=auth.require();m.addAttribute("events",events.list().stream().filter(e->auth.canManage(e.getId())).toList());m.addAttribute("general",user.general());return "account/organizer";}
 @GetMapping("/organizer/access") public String access(Model m){if(!auth.require().general())throw new org.springframework.security.access.AccessDeniedException("Solo administración general");m.addAttribute("events",events.list());return "account/access";}
 @PostMapping("/organizer/access") public String assign(@RequestParam String email,@RequestParam long eventId,@RequestParam(defaultValue="true") boolean grant){long actor=auth.require().id();if(!auth.require().general())throw new org.springframework.security.access.AccessDeniedException("Solo administración general");long target=accounts.find(AccountService.email(email)).orElseThrow(()->new com.ping.web.BusinessRuleException("No encontramos esa cuenta. Debe registrarse primero."));accounts.assign(actor,target,eventId,grant);return "redirect:/organizer/access?saved=true";}
}
