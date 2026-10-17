package com.ping.reward;
import com.ping.event.EventService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
@Controller @Profile("!bootstrap") @RequestMapping("/events/{event}/rewards")
public class BenefitAdminController {
 private final BenefitAdminService service;private final EventService events;
 public BenefitAdminController(BenefitAdminService service,EventService events){this.service=service;this.events=events;}
 @GetMapping public String list(@PathVariable long event,Model m){m.addAttribute("event",events.get(event));m.addAttribute("benefits",service.list(event));return "rewards/manage";}
 @GetMapping("/new") public String create(@PathVariable long event,Model m){m.addAttribute("event",events.get(event));m.addAttribute("benefitForm",new BenefitForm());return "rewards/form";}
 @GetMapping("/{id}/edit") public String edit(@PathVariable long event,@PathVariable long id,Model m){m.addAttribute("event",events.get(event));m.addAttribute("benefitForm",service.form(event,id));m.addAttribute("itemId",id);return "rewards/form";}
 @PostMapping({"","/{id}/edit"}) public String save(@PathVariable long event,@PathVariable(required=false) Long id,@Valid @ModelAttribute BenefitForm benefitForm,BindingResult errors,Model m){if(errors.hasErrors()){m.addAttribute("event",events.get(event));m.addAttribute("itemId",id);return "rewards/form";}service.save(event,id,benefitForm);return "redirect:/events/"+event+"/rewards";}
}
