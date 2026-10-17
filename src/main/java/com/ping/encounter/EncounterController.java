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
 private void context(long event,Model m){m.addAttribute("event",events.get(event));m.addAttribute("points",service.points(event));m.addAttribute("activities",service.activities(event));m.addAttribute("interests",interests.catalog());}
 @GetMapping public String list(@PathVariable long event,Model m){context(event,m);return "encounter/list";}
 @PostMapping("/import") public String importLegacy(@PathVariable long event){service.importLegacy(event);return back(event);}
 @GetMapping("/points/new") public String newPoint(@PathVariable long event,Model m){context(event,m);m.addAttribute("pointForm",new PointForm());return "encounter/point";}
 @GetMapping("/points/{id}/edit") public String editPoint(@PathVariable long event,@PathVariable long id,Model m){context(event,m);m.addAttribute("pointForm",service.pointForm(event,id));m.addAttribute("itemId",id);return "encounter/point";}
 @PostMapping({"/points","/points/{id}/edit"}) public String savePoint(@PathVariable long event,@PathVariable(required=false) Long id,@Valid @ModelAttribute PointForm pointForm,BindingResult errors,Model m){if(errors.hasErrors()){context(event,m);m.addAttribute("itemId",id);return "encounter/point";}service.savePoint(event,id,pointForm);return back(event);}
 @GetMapping("/activities/new") public String newActivity(@PathVariable long event,Model m){context(event,m);m.addAttribute("activityForm",new ActivityForm());return "encounter/activity";}
 @GetMapping("/activities/{id}/edit") public String editActivity(@PathVariable long event,@PathVariable long id,Model m){context(event,m);m.addAttribute("activityForm",service.activityForm(event,id));m.addAttribute("itemId",id);return "encounter/activity";}
 @PostMapping({"/activities","/activities/{id}/edit"}) public String saveActivity(@PathVariable long event,@PathVariable(required=false) Long id,@Valid @ModelAttribute ActivityForm activityForm,BindingResult errors,Model m){if(errors.hasErrors()){context(event,m);m.addAttribute("itemId",id);return "encounter/activity";}service.saveActivity(event,id,activityForm);return back(event);}
 private String back(long event){return "redirect:/events/"+event+"/encounters";}
}
