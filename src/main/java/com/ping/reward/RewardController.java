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
    private final com.ping.verification.CredentialService codes;
    private final com.ping.event.EventService events;
    private final RewardService rewards;
    private final com.ping.match.MatchService matches;
    private final ParticipantService participants;
    public RewardController(RewardService rewards, ParticipantService participants, com.ping.match.MatchService matches,com.ping.verification.CredentialService codes,com.ping.event.EventService events) { this.events=events; this.codes=codes; this.rewards = rewards; this.participants = participants; this.matches = matches; }
    @GetMapping("/me/benefits") public String benefits(HttpSession session, Model model) {
        long self = DemoSession.participantId(session); var participant = participants.get(self); model.addAttribute("participant", participant);model.addAttribute("benefitEvent",events.get(participant.eventId()));
        var experience = com.ping.experience.EventExperienceCatalog.forEvent(participant.eventName());
        model.addAttribute("experience", experience);
        var completion = matches.completionState(self);
        if (experience != null) model.addAttribute("promoUnlocked", completion.completed());
        model.addAttribute("benefits", rewards.catalog(participant.eventId())); model.addAttribute("claims", rewards.personal(self));
        model.addAttribute("completionPending", completion.pending()); model.addAttribute("requestKey", java.util.UUID.randomUUID().toString()); return "rewards/benefits";
    }
    @PostMapping("/me/benefits") public String request(@RequestParam long benefitId, @RequestParam String requestKey, HttpSession session, RedirectAttributes flash) {
        long self=DemoSession.participantId(session);long id=rewards.request(self, benefitId, requestKey);
        if(!"demo".equals(session.getAttribute("pingEntryMode"))){var old=com.ping.verification.CredentialSession.get(session,"REDEMPTION",id,self);if(old==null||!codes.valid(old))try{com.ping.verification.CredentialSession.put(session,codes.redemption(id,self,java.util.UUID.randomUUID().toString()));}catch(com.ping.web.BusinessRuleException issueUnavailable){/* Paid receipt remains recoverable without a second debit. */}return "redirect:/me/claims/"+id;}
         flash.addFlashAttribute("message", "Canje solicitado. El costo se descontó una sola vez; consulta aquí la entrega."); return "redirect:/me/benefits";
    }
    @PostMapping("/admin/claims/{id}") public String finish(@PathVariable long id, @RequestParam ClaimStatus status, RedirectAttributes flash) {
        long eventId = rewards.finish(id,status); flash.addFlashAttribute("message", "Resultado del canje guardado. No se descontaron puntos adicionales."); return "redirect:/admin?eventId=" + eventId;
    }
}
