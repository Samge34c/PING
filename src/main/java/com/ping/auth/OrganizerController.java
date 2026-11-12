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
 @GetMapping("/organizer") public String index(@RequestParam(name="event",required=false) Long requested,jakarta.servlet.http.HttpSession session,Model m){Long chosen=requested;var user=auth.require();
  // Remember the event being managed so «Mis eventos» returns to it; only an id, re-checked against permissions on every request.
  if(chosen==null&&session.getAttribute("pingOrganizerEvent") instanceof Long remembered&&auth.canManage(remembered))chosen=remembered;
  final Long event=chosen;var managed=events.list().stream().filter(e->auth.canManage(e.getId())).toList();m.addAttribute("events",managed);m.addAttribute("general",user.general());
  var selected=managed.stream().filter(e->event!=null&&e.getId().equals(event)).findFirst().orElse(managed.stream().filter(e->e.getStatus()!=com.ping.event.EventStatus.CLOSED).findFirst().orElse(managed.isEmpty()?null:managed.getFirst()));
  if(event!=null&&(selected==null||!selected.getId().equals(event)))throw new org.springframework.security.access.AccessDeniedException("No tienes permiso para administrar este evento.");
  if(selected!=null){session.setAttribute("pingOrganizerEvent",selected.getId());m.addAttribute("board",OrganizerBoard.of(selected,encounters,rewards,reviews,null));m.addAttribute("canOrganize",auth.canOrganize(selected.getId()));m.addAttribute("entryLink",links.link(selected));}
  return "account/organizer";}
 /** DEC-069: read-only guide for organization and staff; same access as /organizer. Keeps the remembered event for «Volver». */
 @GetMapping("/organizer/help") public String help(jakarta.servlet.http.HttpSession session,Model m){auth.require();
  if(session.getAttribute("pingOrganizerEvent") instanceof Long remembered&&auth.canManage(remembered))m.addAttribute("helpEvent",events.get(remembered));
  return "account/organizer-help";}
 @GetMapping("/organizer/access") public String access(Model m,jakarta.servlet.http.HttpSession session){if(!auth.require().general())throw new org.springframework.security.access.AccessDeniedException("Solo administración general");m.addAttribute("events",events.list());
  // Arriving from an event board: that event is preselected (only its id is remembered in the session).
  if(!m.containsAttribute("selectedEvent")&&session.getAttribute("pingOrganizerEvent") instanceof Long remembered)m.addAttribute("selectedEvent",remembered);return "account/access";}
 @PostMapping("/organizer/access") public String assign(@RequestParam String email,@RequestParam long eventId,@RequestParam(defaultValue="true") boolean grant,@RequestParam(defaultValue="ORGANIZER") AccountService.EventRole role,org.springframework.web.servlet.mvc.support.RedirectAttributes flash){long actor=auth.require().id();if(!auth.require().general())throw new org.springframework.security.access.AccessDeniedException("Solo administración general");
  flash.addFlashAttribute("selectedEvent",eventId);
  var target=accounts.find(AccountService.email(email));
  // Unknown account: stay on the form with the typed address and say what to do, instead of a generic error page.
  if(target.isEmpty()){flash.addFlashAttribute("accessError","No encontramos una cuenta con ese correo. La persona debe crear su cuenta y verificarla primero.");flash.addFlashAttribute("accessEmail",email.strip());return "redirect:/organizer/access";}
  accounts.assign(actor,target.get(),eventId,grant,role);
  flash.addFlashAttribute("accessSaved",(grant?(role==AccountService.EventRole.STAFF?"Permiso de personal (revisar y entregar) asignado a ":"Permiso de organización asignado a "):"Permiso retirado a ")+AccountService.email(email)+" en «"+events.get(eventId).getName()+"».");return "redirect:/organizer/access";}
}
