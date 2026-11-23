package com.ping.admin;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.ping.web.NotFoundException;
/**
 * Results (DEC-079, proposal B «Informe de participación»). Only the organisation of each event and general administration
 * see them: STAFF has the board, reviews and deliveries, never results (checked here and in AccessPolicy).
 */
@Controller @Profile("!bootstrap")
public class AdminController {
    private final com.ping.moderation.ModerationService moderation;
    private final com.ping.reward.RewardService rewards;
    private final com.ping.auth.AuthSupport auth;
    private final AdminService service;
    public AdminController(AdminService service, com.ping.moderation.ModerationService moderation, com.ping.reward.RewardService rewards,com.ping.auth.AuthSupport auth) { this.auth=auth; this.service = service; this.moderation = moderation; this.rewards = rewards; }
    private boolean allowed(long event) { return auth.legacyFixture() || auth.canOrganize(event); }
    @GetMapping("/admin") public String dashboard(@RequestParam(required = false) Long eventId, Model model, jakarta.servlet.http.HttpSession session) {
        var all = service.metrics().stream().filter(e -> allowed(e.id())).toList();
        var selected = eventId == null ? all : all.stream().filter(e -> e.id() == eventId).toList();
        if (eventId != null && selected.isEmpty()) throw new NotFoundException("No encontramos ese evento. Vuelve al panel y selecciona uno registrado.");
        model.addAttribute("allEvents", all); model.addAttribute("selectedEventId", eventId);
        if (eventId != null) {
            model.addAttribute("rewards", selected.stream().collect(java.util.stream.Collectors.toMap(EventMetrics::id, e -> rewards.forEvent(e.id()))));
            // One event: the full report with the existing detail (participants, encounters, reports, claims).
            session.setAttribute("pingOrganizerEvent", eventId);
            model.addAttribute("reports", selected.stream().collect(java.util.stream.Collectors.toMap(EventMetrics::id, e -> moderation.forEvent(e.id()))));
            model.addAttribute("details", selected.stream().collect(java.util.stream.Collectors.toMap(EventMetrics::id, e -> service.details(e.id()))));
        }
        model.addAttribute("events", selected); return "admin/dashboard";
    }
    /** DEC-079: optional door total of the same event. Blank removes it; nothing is estimated or back-filled. */
    @PostMapping("/events/{id}/attendance") public String door(@PathVariable long id, @RequestParam(required = false) String doorAttendance, RedirectAttributes flash) {
        if (!allowed(id)) throw new AccessDeniedException("Solo la organización de este evento o administración general registran el total de puerta.");
        String typed = doorAttendance == null ? "" : doorAttendance.strip();
        try {
            // Validate what was typed: digits, or thousands correctly grouped with «.» or a space («1.200»). «12.5» or «...» are rejected, never reinterpreted.
            if (!typed.isEmpty() && !typed.matches("[0-9]{1,7}|[0-9]{1,3}([. ][0-9]{3}){1,2}")) throw new com.ping.web.BusinessRuleException("Escribe solo el número de personas, sin letras ni decimales.");
            String raw = typed.replace(".", "").replace(" ", "");
            if (raw.length() > 7) throw new com.ping.web.BusinessRuleException("Escribe solo el número de personas, sin letras ni decimales.");
            service.recordDoor(id, raw.isEmpty() ? null : Integer.valueOf(raw));
            flash.addFlashAttribute("doorSaved", raw.isEmpty() ? "Total de puerta quitado." : "Total de puerta guardado.");
        } catch (com.ping.web.BusinessRuleException ex) { flash.addFlashAttribute("doorError", ex.getMessage()); flash.addFlashAttribute("doorTyped", doorAttendance); }
        return "redirect:/admin?eventId=" + id + "#puerta";
    }
}
