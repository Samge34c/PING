package com.ping.auth;
import java.util.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import com.ping.participant.ParticipantService;
import com.ping.web.BusinessRuleException;
@Controller @Profile("!bootstrap")
public class AccountController {
 private final AccountService accounts;private final AuthSupport auth;private final PrivateMail mail;private final AuthThrottle throttle;private final AuthConfiguration.ProviderCatalog providers;private final ParticipantService people;
 public AccountController(AccountService a,AuthSupport s,PrivateMail m,AuthThrottle t,AuthConfiguration.ProviderCatalog p,ParticipantService people){accounts=a;auth=s;mail=m;throttle=t;providers=p;this.people=people;}
 @GetMapping("/login") public String login(@RequestParam(required=false) String next,HttpSession s,Model m){if(next!=null)s.setAttribute("pingReturnTo",auth.next(next));m.addAttribute("providers",providers.available());return "account/login";}
 @GetMapping("/register") public String register(Model m){m.addAttribute("mailAvailable",mail.available());return "account/register";}
 @PostMapping("/register") public String register(@RequestParam String email,@RequestParam String password,@RequestParam String repeat,HttpServletRequest r,Model m){m.addAttribute("mailAvailable",mail.available());try{throttle.check("register-ip",r.getRemoteAddr(),5,15);if(!password.equals(repeat))throw new BusinessRuleException("Las contraseñas no coinciden.");accounts.register(email,password);m.addAttribute("sent",true);}catch(BusinessRuleException ex){m.addAttribute("formError",ex.getMessage());m.addAttribute("email",email);}return "account/register";}
 @GetMapping("/account/recover") public String recover(Model m){m.addAttribute("mailAvailable",mail.available());return "account/recover";}
 @PostMapping("/account/recover") public String recover(@RequestParam String email,HttpServletRequest r,Model m){try{throttle.check("recover-ip",r.getRemoteAddr(),10,15);throttle.check("recover-account",AccountService.email(email),5,15);accounts.recover(email);m.addAttribute("sent",true);}catch(BusinessRuleException ex){m.addAttribute("formError",ex.getMessage());}m.addAttribute("mailAvailable",mail.available());return "account/recover";}
 @GetMapping({"/account/verify","/account/reset"}) public String token(@RequestParam(defaultValue="") String token,HttpServletRequest r,Model m){m.addAttribute("token",token);m.addAttribute("reset",r.getRequestURI().endsWith("reset"));return "account/token";}
 @PostMapping({"/account/verify","/account/reset"}) public String consume(@RequestParam String token,@RequestParam(defaultValue="") String password,@RequestParam(defaultValue="") String repeat,HttpServletRequest r,Model m){boolean reset=r.getRequestURI().endsWith("reset");try{throttle.check("token-ip",r.getRemoteAddr(),20,15);if(reset&&!password.equals(repeat))throw new BusinessRuleException("Las contraseñas no coinciden.");accounts.consume(token,reset?"RESET":"VERIFY",password);return "redirect:/login?verified=true";}catch(BusinessRuleException ex){m.addAttribute("token",token);m.addAttribute("reset",reset);m.addAttribute("formError",ex.getMessage());return "account/token";}}
 @GetMapping("/account") public String account(Model m){var a=auth.require();m.addAttribute("account",a);m.addAttribute("participations",accounts.participations(a.id()));m.addAttribute("providers",providers.available());return "account/profile";}
 @PostMapping("/account/participations/{event}") public String select(@PathVariable long event,HttpSession s){long id=accounts.participant(auth.require().id(),event).orElseThrow(()->new BusinessRuleException("No tienes participación en ese evento."));auth.entered(s,id);return "redirect:/me";}
 @GetMapping("/account/link-session") public String legacy(HttpSession s,Model m){auth.require();Object id=s.getAttribute("pingLegacyOwnedParticipant");if(!(id instanceof Long))return "redirect:/account";m.addAttribute("legacy",people.get((Long)id));return "account/link-session";}
 @PostMapping("/account/link-session") public String legacy(HttpSession s,@RequestParam(defaultValue="false") boolean confirm){var a=auth.require();Object raw=s.getAttribute("pingLegacyOwnedParticipant");if(!(raw instanceof Long id))throw new BusinessRuleException("La sesión anterior ya no está disponible.");if(confirm){var p=people.get(id);accounts.bind(a.id(),id,p.eventId());auth.entered(s,id);}s.removeAttribute("pingLegacyOwnedParticipant");return "redirect:/account";}
 @PostMapping("/account/link/{provider}") public String provider(@PathVariable String provider,HttpSession s){var a=auth.require();if(!providers.available().contains(provider))throw new BusinessRuleException("Proveedor no configurado.");s.setAttribute("pingLinkAccount",a.id());s.setAttribute("pingLinkUntil",System.currentTimeMillis()+300000L);return "redirect:/oauth2/authorization/"+provider;}
}
