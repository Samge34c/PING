package com.ping.admin;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.ping.web.NotFoundException;
@Controller @Profile("!bootstrap")
public class AdminController {
    private final AdminService service;
    public AdminController(AdminService service) { this.service = service; }
    @GetMapping("/admin") public String dashboard(@RequestParam(required = false) Long eventId, Model model) {
        var all = service.metrics();
        var selected = eventId == null ? all : all.stream().filter(e -> e.id() == eventId).toList();
        if (eventId != null && selected.isEmpty()) throw new NotFoundException("No encontramos ese evento. Vuelve al panel y selecciona uno registrado.");
        model.addAttribute("allEvents", all); model.addAttribute("selectedEventId", eventId);
        model.addAttribute("events", selected); return "admin/dashboard";
    }
}
