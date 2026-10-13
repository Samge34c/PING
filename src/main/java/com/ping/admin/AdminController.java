package com.ping.admin;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
@Controller @Profile("!bootstrap")
public class AdminController {
    private final AdminService service;
    public AdminController(AdminService service) { this.service = service; }
    @GetMapping("/admin") public String dashboard(Model model) { model.addAttribute("events", service.metrics()); return "admin/dashboard"; }
}
