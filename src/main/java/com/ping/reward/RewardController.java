package com.ping.reward;
import com.ping.match.DemoSession;
import com.ping.participant.ParticipantService;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller @Profile("!bootstrap")
public class RewardController {
    private final RewardService rewards;
    private final com.ping.match.MatchService matches;
    private final ParticipantService participants;
    public RewardController(RewardService rewards, ParticipantService participants, com.ping.match.MatchService matches) { this.rewards = rewards; this.participants = participants; this.matches = matches; }
    @GetMapping("/me/benefits") public String benefits(HttpSession session, Model model) {
        long self = DemoSession.participantId(session); var participant = participants.get(self); model.addAttribute("participant", participant);
        var experience = com.ping.experience.EventExperienceCatalog.forEvent(participant.eventName());
        model.addAttribute("experience", experience);
        if (experience != null) model.addAttribute("promoUnlocked", com.ping.experience.EventExperienceCatalog.promoUnlocked(matches.list(self)));
        model.addAttribute("benefits", rewards.catalog()); model.addAttribute("claims", rewards.personal(self));
        model.addAttribute("completionPending", rewards.pendingCompletion(self)); model.addAttribute("requestKey", java.util.UUID.randomUUID().toString()); return "rewards/benefits";
    }
    @PostMapping("/me/benefits") public String request(@RequestParam long benefitId, @RequestParam String requestKey, HttpSession session, RedirectAttributes flash) {
        rewards.request(DemoSession.participantId(session), benefitId, requestKey); flash.addFlashAttribute("message", "Canje solicitado. El costo se descontó una sola vez; consulta aquí la entrega."); return "redirect:/me/benefits";
    }
    @PostMapping("/admin/claims/{id}") public String finish(@PathVariable long id, @RequestParam ClaimStatus status, RedirectAttributes flash) {
        long eventId = rewards.finish(id,status); flash.addFlashAttribute("message", "Resultado del canje guardado. No se descontaron puntos adicionales."); return "redirect:/admin?eventId=" + eventId;
    }
}
