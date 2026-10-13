package com.ping.match;

import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.ping.participant.*;
import com.ping.web.*;

@Controller @Profile("!bootstrap")
public class DemoController {
    private static final String SELECTED = "pingDemoParticipantId";
    private final ParticipantService participants;
    private final MatchingService matching;
    private final MatchService matches;
    public DemoController(ParticipantService participants, MatchingService matching, MatchService matches) {
        this.participants = participants; this.matching = matching; this.matches = matches;
    }
    @GetMapping("/demo") public String select(@RequestParam(required = false) Long participantId, Model model) { model.addAttribute("selectedParticipantId", participantId); model.addAttribute("participants", participants.list()); return "demo/select"; }
    @PostMapping("/demo") public String enter(@RequestParam long participantId, HttpSession session) {
        participants.get(participantId); session.setAttribute(SELECTED, participantId); return "redirect:/me";
    }
    @PostMapping("/demo/exit") public String exit(HttpSession session) { session.removeAttribute(SELECTED); return "redirect:/demo"; }
    private long self(HttpSession session) {
        Object id = session.getAttribute(SELECTED);
        if (!(id instanceof Long selected)) throw new BusinessRuleException("Entra como participante desde Modo demo antes de continuar.");
        return selected;
    }
    @GetMapping("/me") public String dashboard(HttpSession session, Model model) {
        if (session.getAttribute(SELECTED) == null) return "redirect:/demo";
        long id = self(session); var history = matches.list(id);
        model.addAttribute("participant", participants.get(id)); model.addAttribute("matches", history);
        model.addAttribute("held", history.stream().anyMatch(m -> m.status() == MatchStatus.OFFERED || m.status() == MatchStatus.ACTIVE));
        model.addAttribute("completedCount", matches.completedCount()); return "demo/dashboard";
    }
    @PostMapping("/me/availability") public String availability(@ModelAttribute AvailabilityForm form, HttpSession session) {
        participants.setAvailability(self(session), form); return "redirect:/me";
    }
    @PostMapping("/me/search") public String search(HttpSession session, RedirectAttributes flash) {
        long id = self(session);
        try { return "redirect:/matches/" + matching.search(id); }
        catch (BusinessRuleException error) { flash.addFlashAttribute("message", error.getMessage()); return "redirect:/me"; }
    }
    @GetMapping("/matches/{id}") public String match(@PathVariable long id, HttpSession session, Model model) {
        long selected = self(session); model.addAttribute("participant", participants.get(selected));
        model.addAttribute("match", matches.get(id, selected)); return "demo/match";
    }
    @PostMapping("/matches/{id}/accept") public String accept(@PathVariable long id, HttpSession session) { matches.accept(id, self(session)); return "redirect:/matches/" + id; }
    @PostMapping("/matches/{id}/reject") public String reject(@PathVariable long id, HttpSession session) { matches.cancel(id, self(session)); return "redirect:/matches/" + id; }
    @PostMapping("/matches/{id}/cancel") public String cancel(@PathVariable long id, HttpSession session) { matches.cancel(id, self(session)); return "redirect:/matches/" + id; }
    @PostMapping("/matches/{id}/confirm") public String confirm(@PathVariable long id, HttpSession session) { matches.confirm(id, self(session)); return "redirect:/matches/" + id; }
    @PostMapping("/matches/{id}/feedback") public String feedback(@PathVariable long id, @RequestParam String rating, HttpSession session) {
        Feedback.Rating selected;
        try { selected = Feedback.Rating.valueOf(rating); }
        catch (IllegalArgumentException error) { throw new BusinessRuleException("Selecciona una opción válida de feedback."); }
        matches.giveFeedback(id, self(session), selected); return "redirect:/matches/" + id;
    }
}
