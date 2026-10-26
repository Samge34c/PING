package com.ping.participant;
import jakarta.servlet.http.*;
import com.ping.web.*;
import org.springframework.stereotype.Controller;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.ui.Model;
@Controller @Profile("!bootstrap")
public class AvatarController{
 private final ParticipantService participants;
 public AvatarController(ParticipantService p){participants=p;}
 @ModelAttribute public void presets(Model m){m.addAttribute("avatarAnimals",Avatar.ANIMALS);m.addAttribute("avatarHats",Avatar.HATS);m.addAttribute("avatarAccessories",Avatar.ACCESSORIES);m.addAttribute("avatarColors",Avatar.COLORS);}
 private long self(HttpSession s){if(!(s.getAttribute("pingDemoParticipantId") instanceof Long id))throw new SessionRequiredException();return (Long)s.getAttribute("pingDemoParticipantId");}
 private String editor(Avatar current,AvatarV3 draft,Model m){
  m.addAttribute("avatar",current);m.addAttribute("legacyPreset",current.v3()==null);m.addAttribute("legacyEquivalent",AvatarV3.hasEquivalent(current));m.addAttribute("avatarSaved",current.v3());
  AvatarCatalog.addTo(m,draft);return "entry/avatar-edit";
 }
 @GetMapping("/me/avatar") public String edit(HttpSession s,Model m){var current=participants.avatar(self(s));return editor(current,current.v3()!=null?current.v3():AvatarV3.suggestionFor(current),m);}
 /** V3 (DEC-058): palette identifica el formulario nuevo. Sin palette se acepta el formulario del catálogo anterior, por compatibilidad. */
 @PostMapping("/me/avatar") public String save(HttpSession s,Model m,HttpServletResponse response,RedirectAttributes flash,
   @RequestParam(defaultValue="gato") String animal,@RequestParam(required=false) String palette,
   @RequestParam(defaultValue="none") String head,@RequestParam(required=false) String headColor,@RequestParam(defaultValue="none") String audio,@RequestParam(required=false) String audioColor,
   @RequestParam(defaultValue="none") String face,@RequestParam(required=false) String faceColor,@RequestParam(defaultValue="none") String neck,@RequestParam(required=false) String neckColor,
   @RequestParam(defaultValue="none") String hat,@RequestParam(defaultValue="none") String accessory,@RequestParam(defaultValue="violeta") String background){
  long id=self(s);var current=participants.avatar(id);
  if(palette==null){participants.avatar(id,new Avatar(animal,hat,accessory,background));return "redirect:/me";}
  try{participants.avatar(id,current.withV3(new AvatarV3(animal,palette,head,headColor,audio,audioColor,face,faceColor,neck,neckColor)));}
  catch(BusinessRuleException ex){response.setStatus(400);m.addAttribute("avatarError",ex.getMessage());return editor(current,AvatarV3.lenient(animal,palette,head,headColor,audio,audioColor,face,faceColor,neck,neckColor),m);}
  flash.addFlashAttribute("message","Avatar guardado.");return "redirect:/me";
 }
}
