package com.ping.encounter;
import com.ping.event.EventService;
import com.ping.interest.InterestService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
@Controller @Profile("!bootstrap") @RequestMapping("/events/{event}/encounters")
public class EncounterController {
 private final EncounterService service;private final EventService events;private final InterestService interests;
 public EncounterController(EncounterService service,EventService events,InterestService interests){this.service=service;this.events=events;this.interests=interests;}
 public void context(long event,Model m){m.addAttribute("pointCount",service.points(event).stream().filter(p->!p.isArchived()).count());m.addAttribute("activityCount",service.activities(event).stream().filter(a->!a.isArchived()).count());m.addAttribute("words",com.ping.event.EncounterOptions.unique(events.get(event).getEncounterWords()));if(!m.containsAttribute("pointForm"))m.addAttribute("pointForm",new PointForm());if(!m.containsAttribute("activityForm"))m.addAttribute("activityForm",new ActivityForm());m.addAttribute("event",events.get(event));m.addAttribute("points",service.points(event));m.addAttribute("activities",service.activities(event));m.addAttribute("interests",interests.catalog());}
 @GetMapping public String list(@PathVariable long event,@RequestParam(required=false) Long point,@RequestParam(required=false) Long activity,Model m){context(event,m);if(point!=null){m.addAttribute("pointForm",service.pointForm(event,point));m.addAttribute("pointId",point);m.addAttribute("pointOpen",true);}if(activity!=null){m.addAttribute("activityForm",service.activityForm(event,activity));m.addAttribute("activityId",activity);m.addAttribute("activityOpen",true);}return "encounter/list";}
 @PostMapping("/import") public String importLegacy(@PathVariable long event){service.importLegacy(event);return back(event);}
 @GetMapping("/points/new") public String newPoint(@PathVariable long event,Model m){context(event,m);m.addAttribute("pointForm",new PointForm());return "encounter/point";}
 @GetMapping("/points/{id}/edit") public String editPoint(@PathVariable long event,@PathVariable long id,Model m){context(event,m);m.addAttribute("pointForm",service.pointForm(event,id));m.addAttribute("itemId",id);return "encounter/point";}
 @PostMapping({"/points","/points/{id}/edit"}) public String savePoint(@PathVariable long event,@PathVariable(required=false) Long id,@Valid @ModelAttribute PointForm pointForm,BindingResult errors,Model m){if(!errors.hasErrors())try{service.savePoint(event,id,pointForm);return back(event);}catch(com.ping.web.BusinessRuleException ex){errors.reject("rule",ex.getMessage());}context(event,m);m.addAttribute("pointId",id);m.addAttribute("pointOpen",true);return "encounter/list";}
 @GetMapping("/activities/new") public String newActivity(@PathVariable long event,Model m){context(event,m);m.addAttribute("activityForm",new ActivityForm());return "encounter/activity";}
 @GetMapping("/activities/{id}/edit") public String editActivity(@PathVariable long event,@PathVariable long id,Model m){context(event,m);m.addAttribute("activityForm",service.activityForm(event,id));m.addAttribute("itemId",id);return "encounter/activity";}
 @PostMapping({"/activities","/activities/{id}/edit"}) public String saveActivity(@PathVariable long event,@PathVariable(required=false) Long id,@Valid @ModelAttribute ActivityForm activityForm,BindingResult errors,Model m){if(!errors.hasErrors())try{service.saveActivity(event,id,activityForm);return back(event);}catch(com.ping.web.BusinessRuleException ex){errors.reject("rule",ex.getMessage());}context(event,m);m.addAttribute("activityId",id);m.addAttribute("activityOpen",true);return "encounter/list";}
 @PostMapping("/points/{id}/archive") public String archivePoint(@PathVariable long event,@PathVariable long id){service.archivePoint(event,id);return back(event);}
 @PostMapping("/activities/{id}/archive") public String archiveActivity(@PathVariable long event,@PathVariable long id){service.archiveActivity(event,id);return back(event);}
 @PostMapping("/words") public String words(@PathVariable long event,@RequestParam String words,@RequestParam long version,Model m){try{service.words(event,words,version);return back(event);}catch(com.ping.web.BusinessRuleException ex){context(event,m);m.addAttribute("wordsError",ex.getMessage());m.addAttribute("wordDraft",words);return "encounter/list";}}
 private String back(long event){return "redirect:/events/"+event+"/encounters";}
}
