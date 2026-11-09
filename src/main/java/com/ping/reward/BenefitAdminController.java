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
 private final BenefitAdminService service;private final EventService events;private final BenefitImageService images;
 public BenefitAdminController(BenefitAdminService service,EventService events,BenefitImageService images){this.service=service;this.events=events;this.images=images;}
 @GetMapping public String list(@PathVariable long event,Model m){m.addAttribute("event",events.get(event));m.addAttribute("benefits",service.list(event));return "rewards/manage";}
 @GetMapping("/new") public String create(@PathVariable long event,Model m){m.addAttribute("event",events.get(event));m.addAttribute("benefitForm",new BenefitForm());return "rewards/form";}
 @GetMapping("/{id}/edit") public String edit(@PathVariable long event,@PathVariable long id,Model m){m.addAttribute("event",events.get(event));m.addAttribute("benefitForm",service.form(event,id));m.addAttribute("itemId",id);m.addAttribute("benefit",images.benefit(id));return "rewards/form";}
 @PostMapping("/{id}/image") public String image(@PathVariable long event,@PathVariable long id,@RequestParam(required=false) org.springframework.web.multipart.MultipartFile image,org.springframework.web.servlet.mvc.support.RedirectAttributes flash){try{images.save(event,id,image);flash.addFlashAttribute("message","Imagen guardada. Así la verán los participantes.");}catch(com.ping.web.BusinessRuleException e){flash.addFlashAttribute("imageError",e.getMessage());}return "redirect:/events/"+event+"/rewards/"+id+"/edit#imagen";}
 @PostMapping("/{id}/image/remove") public String removeImage(@PathVariable long event,@PathVariable long id,org.springframework.web.servlet.mvc.support.RedirectAttributes flash){images.remove(event,id);flash.addFlashAttribute("message","Imagen retirada. La recompensa se conserva y mostrará la imagen predeterminada de PING.");return "redirect:/events/"+event+"/rewards/"+id+"/edit#imagen";}
 @GetMapping("/{id}/image") public org.springframework.http.ResponseEntity<byte[]> preview(@PathVariable long event,@PathVariable long id){images.requireOwned(event,id);return BenefitImages.response(images.read(id));}
 @PostMapping({"","/{id}/edit"}) public String save(@PathVariable long event,@PathVariable(required=false) Long id,@Valid @ModelAttribute BenefitForm benefitForm,BindingResult errors,Model m){if(errors.hasErrors()){m.addAttribute("event",events.get(event));m.addAttribute("itemId",id);if(id!=null)m.addAttribute("benefit",images.benefit(id));return "rewards/form";}service.save(event,id,benefitForm);return "redirect:/events/"+event+"/rewards";}
}
