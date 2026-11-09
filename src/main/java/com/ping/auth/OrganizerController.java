package com.ping.auth;
import org.springframework.stereotype.Controller;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import com.ping.event.EventService;
@Controller @Profile("!bootstrap")
public class OrganizerController {
 private final AuthSupport auth;private final AccountService accounts;private final EventService events;
 private final com.ping.encounter.EncounterService encounters;private final com.ping.reward.RewardService rewards;private final com.ping.review.ActivityReviewService reviews;private final com.ping.event.EventEntryLinks links;
 public OrganizerController(AuthSupport a,AccountService s,EventService e,com.ping.encounter.EncounterService encounters,com.ping.reward.RewardService rewards,com.ping.review.ActivityReviewService reviews,com.ping.event.EventEntryLinks links){auth=a;accounts=s;events=e;this.encounters=encounters;this.rewards=rewards;this.reviews=reviews;this.links=links;}
 /** DEC-066: the page is organised around one selected event; the selector is always visible when there are several. */
 @GetMapping("/organizer") public String index(@RequestParam(required=false) Long event,Model m){var user=auth.require();var managed=events.list().stream().filter(e->auth.canManage(e.getId())).toList();m.addAttribute("events",managed);m.addAttribute("general",user.general());
  var selected=managed.stream().filter(e->event!=null&&e.getId().equals(event)).findFirst().orElse(managed.stream().filter(e->e.getStatus()!=com.ping.event.EventStatus.CLOSED).findFirst().orElse(managed.isEmpty()?null:managed.getFirst()));
  if(event!=null&&(selected==null||!selected.getId().equals(event)))throw new org.springframework.security.access.AccessDeniedException("No tienes permiso para administrar este evento.");
  if(selected!=null){m.addAttribute("board",OrganizerBoard.of(selected,encounters,rewards,reviews,null));m.addAttribute("entryLink",links.link(selected));}
  return "account/organizer";}
 @GetMapping("/organizer/access") public String access(Model m){if(!auth.require().general())throw new org.springframework.security.access.AccessDeniedException("Solo administración general");m.addAttribute("events",events.list());return "account/access";}
 @PostMapping("/organizer/access") public String assign(@RequestParam String email,@RequestParam long eventId,@RequestParam(defaultValue="true") boolean grant){long actor=auth.require().id();if(!auth.require().general())throw new org.springframework.security.access.AccessDeniedException("Solo administración general");long target=accounts.find(AccountService.email(email)).orElseThrow(()->new com.ping.web.BusinessRuleException("No encontramos esa cuenta. Debe registrarse primero."));accounts.assign(actor,target,eventId,grant);return "redirect:/organizer/access?saved=true";}
}
