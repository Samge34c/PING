package com.ping.auth;
import org.springframework.web.bind.annotation.*;
import org.springframework.context.annotation.Profile;
import org.springframework.ui.Model;
import jakarta.servlet.http.HttpServletRequest;
@ControllerAdvice @Profile("!bootstrap")
public class AuthViewModel{
 private final AuthSupport auth;
 public AuthViewModel(AuthSupport a){auth=a;}
 @ModelAttribute public void flags(HttpServletRequest r,Model m){if(r.getRequestURI().equals("/me/state")||r.getRequestURI().endsWith("recognition-hint"))return;var a=auth.current();m.addAttribute("signedIn",a!=null);m.addAttribute("isOrganizer",auth.legacyFixture()&&!"participant".equals(r.getSession().getAttribute("pingEntryMode"))||a!=null&&(a.general()||a.organizer()));m.addAttribute("isGeneral",auth.legacyFixture()||a!=null&&a.general());m.addAttribute("demoEnabled",auth.legacyFixture()||auth.demo());}
}
