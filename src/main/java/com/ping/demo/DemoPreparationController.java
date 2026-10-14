package com.ping.demo;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller @Profile("!bootstrap")
public class DemoPreparationController {
    private final DemoPreparationService service;
    public DemoPreparationController(DemoPreparationService service) { this.service = service; }
    @GetMapping("/demo/prepare") public String page(Model model) { model.addAttribute("scenario", service.current()); return "demo/prepare"; }
    @PostMapping("/demo/prepare") public String prepare(RedirectAttributes flash) {
        service.prepare(); flash.addFlashAttribute("message", "Escenario demo listo. Se conservan puntos, disponibilidad y conexiones anteriores."); return "redirect:/demo/prepare";
    }
}
