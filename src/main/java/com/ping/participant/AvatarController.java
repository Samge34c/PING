package com.ping.participant;
import jakarta.servlet.http.HttpSession;
import com.ping.web.*;
import org.springframework.stereotype.Controller;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
@Controller @Profile("!bootstrap")
public class AvatarController{
 private final ParticipantService participants;
 public AvatarController(ParticipantService p){participants=p;}
 @ModelAttribute public void presets(Model m){m.addAttribute("avatarAnimals",Avatar.ANIMALS);m.addAttribute("avatarHats",Avatar.HATS);m.addAttribute("avatarAccessories",Avatar.ACCESSORIES);m.addAttribute("avatarColors",Avatar.COLORS);}
 private long self(HttpSession s){if(!(s.getAttribute("pingDemoParticipantId") instanceof Long id))throw new SessionRequiredException();return (Long)s.getAttribute("pingDemoParticipantId");}
 @GetMapping("/me/avatar") public String edit(HttpSession s,Model m){m.addAttribute("avatar",participants.avatar(self(s)));return "entry/avatar-edit";}
 @PostMapping("/me/avatar") public String save(HttpSession s,@RequestParam String animal,@RequestParam String hat,@RequestParam String accessory,@RequestParam String background){participants.avatar(self(s),new Avatar(animal,hat,accessory,background));return "redirect:/me";}
}
