package com.ping.eventextras;
import com.ping.event.*;
import com.ping.encounter.EncounterService;
import com.ping.match.*;
import com.ping.participant.ParticipantService;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.ping.web.BusinessRuleException;
@Controller("eventExtrasController") @Profile("!bootstrap")
public class EventExperienceController {
 private final EventExperienceService extras;private final EventService events;private final EncounterService encounters;private final ParticipantService people;private final MatchService matches;private final ExperienceCard cards;
 public EventExperienceController(EventExperienceService x,EventService e,EncounterService c,ParticipantService p,MatchService m,ExperienceCard card){extras=x;events=e;encounters=c;people=p;matches=m;cards=card;}
 private void event(long id,Model m){m.addAttribute("event",events.get(id));}
 private long own(HttpSession s,Model m){long self=DemoSession.participantId(s);var p=people.get(self);m.addAttribute("participant",p);event(p.eventId(),m);return self;}
 @GetMapping("/events/{id}/preparation") public String preparation(@PathVariable long id,Model m){event(id,m);m.addAttribute("preparation",extras.preparation(id));return "eventextras/preparation";}
 @GetMapping("/events/{id}/experience-settings") public String settings(@PathVariable long id,Model m){event(id,m);m.addAttribute("experienceSettingsForm",extras.settingsForm(id));return "eventextras/settings";}
 @PostMapping("/events/{id}/experience-settings") public String settingsSave(@PathVariable long id,@Valid @ModelAttribute ExperienceSettingsForm f,BindingResult errors,Model m,HttpServletResponse r,RedirectAttributes flash){if(!errors.hasErrors())try{extras.saveSettings(id,f);flash.addFlashAttribute("message","Ayuda y opciones guardadas.");return "redirect:/events/"+id+"/preparation";}catch(BusinessRuleException ex){r.setStatus(409);errors.reject("settings",ex.getMessage());}else r.setStatus(400);event(id,m);return "eventextras/settings";}
 @GetMapping("/events/{id}/agenda") public String manageAgenda(@PathVariable long id,Model m){event(id,m);m.addAttribute("agenda",extras.managedAgenda(id));return "eventextras/agenda-manage";}
 @GetMapping({"/events/{id}/agenda/new","/events/{id}/agenda/{item}/edit"}) public String agendaEdit(@PathVariable long id,@PathVariable(required=false) Long item,Model m){event(id,m);m.addAttribute("agendaForm",extras.agendaForm(id,item));m.addAttribute("itemId",item);m.addAttribute("points",encounters.points(id).stream().filter(p->!p.isArchived()).toList());return "eventextras/agenda-form";}
 @PostMapping({"/events/{id}/agenda","/events/{id}/agenda/{item}/edit"}) public String agendaSave(@PathVariable long id,@PathVariable(required=false) Long item,@Valid @ModelAttribute AgendaForm f,BindingResult errors,Model m,HttpServletResponse r){if(!errors.hasErrors())try{extras.saveAgenda(id,item,f);return "redirect:/events/"+id+"/agenda";}catch(BusinessRuleException ex){r.setStatus(409);errors.reject("agenda",ex.getMessage());}else r.setStatus(400);event(id,m);m.addAttribute("itemId",item);m.addAttribute("points",encounters.points(id).stream().filter(p->!p.isArchived()).toList());return "eventextras/agenda-form";}
 @GetMapping("/me/agenda") public String agenda(HttpSession s,Model m){long self=own(s,m);m.addAttribute("agenda",extras.agenda(people.get(self).eventId()));m.addAttribute("agendaNow",java.time.Instant.now());m.addAttribute("pointNames",encounters.points(people.get(self).eventId()).stream().collect(java.util.stream.Collectors.toMap(p->p.getId(),p->p.getName())));return "eventextras/agenda";}
 @GetMapping("/me/challenges") public String challenges(HttpSession s,Model m){long self=own(s,m);m.addAttribute("personal",extras.personal(self));return "eventextras/challenges";}
 @GetMapping("/me/experience") public String experience(HttpSession s,Model m){long self=own(s,m);m.addAttribute("personal",extras.personal(self));return "eventextras/card";}
 @GetMapping(value="/me/experience/card.svg",produces="image/svg+xml") @ResponseBody public org.springframework.http.ResponseEntity<byte[]> card(HttpSession s,@RequestParam(defaultValue="false") boolean download)throws java.io.IOException{return org.springframework.http.ResponseEntity.ok().header("Cache-Control","no-store, private").header("Content-Disposition",(download?"attachment":"inline")+"; filename=\"mi-experiencia-ping.svg\"").header("X-Content-Type-Options","nosniff").body(cards.render(DemoSession.participantId(s)));}
 @GetMapping("/me/help") public String help(HttpSession s,Model m){long self=own(s,m);m.addAttribute("settings",extras.settings(people.get(self).eventId()));var current=matches.currentId(self).map(id->matches.get(id,self)).orElse(null);m.addAttribute("current",current);String step="Activa tu disponibilidad y busca una propuesta desde Conectar.";
  if(current!=null){if(current.status()==MatchStatus.OFFERED)step="Revisa la propuesta. El encuentro solo comienza si ambos aceptan.";else if(current.completionPending())step="La finalización sigue pendiente. Vuelve al encuentro para consultar su estado o reintentar.";else if(current.verificationPolicy()<2)step="Este encuentro conserva su política anterior. Sigue sus instrucciones; no se añaden verificaciones nuevas.";else if(!current.arrived()||!current.otherArrived())step="Vayan al punto público asignado y cada persona registre su llegada.";else if(!current.verified()||!current.otherVerified())step="Muestra tu código personal y verifica el de la otra persona. La palabra compartida no verifica.";else step="Hagan la actividad asignada. Después, cada persona confirma; la primera confirmación no entrega puntos.";}
  m.addAttribute("step",step);return "eventextras/help";}
}
