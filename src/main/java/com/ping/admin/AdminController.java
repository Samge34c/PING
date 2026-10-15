package com.ping.admin;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.ping.web.NotFoundException;
@Controller @Profile("!bootstrap")
public class AdminController {
    private final com.ping.moderation.ModerationService moderation;
    private final com.ping.reward.RewardService rewards;
    private final AdminService service;
    public AdminController(AdminService service, com.ping.moderation.ModerationService moderation, com.ping.reward.RewardService rewards) { this.service = service; this.moderation = moderation; this.rewards = rewards; }
    @GetMapping("/admin") public String dashboard(@RequestParam(required = false) Long eventId, Model model) {
        var all = service.metrics();
        var selected = eventId == null ? all : all.stream().filter(e -> e.id() == eventId).toList();
        if (eventId != null && selected.isEmpty()) throw new NotFoundException("No encontramos ese evento. Vuelve al panel y selecciona uno registrado.");
        model.addAttribute("allEvents", all); model.addAttribute("selectedEventId", eventId);
        model.addAttribute("rewards", selected.stream().collect(java.util.stream.Collectors.toMap(EventMetrics::id, e -> rewards.forEvent(e.id()))));
        model.addAttribute("reports", selected.stream().collect(java.util.stream.Collectors.toMap(EventMetrics::id, e -> moderation.forEvent(e.id()))));
        model.addAttribute("details", selected.stream().collect(java.util.stream.Collectors.toMap(EventMetrics::id, e -> service.details(e.id()))));
        model.addAttribute("events", selected); return "admin/dashboard";
    }
}
