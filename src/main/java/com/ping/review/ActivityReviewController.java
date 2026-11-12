package com.ping.review;

import jakarta.servlet.http.HttpSession;
import com.ping.match.*;
import com.ping.web.BusinessRuleException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** DEC-066. Participant submission inside /matches (session ownership) and staff review under /events/{id} (event permission). */
@Controller @Profile("!bootstrap")
public class ActivityReviewController {
    private final ActivityReviewService reviews;
    private final MatchService matches;
    private final com.ping.event.EventService events;
    public ActivityReviewController(ActivityReviewService reviews, MatchService matches, com.ping.event.EventService events) {
        this.reviews = reviews; this.matches = matches; this.events = events;
    }

    @PostMapping("/matches/{id}/review")
    public String submit(@PathVariable long id, @RequestParam(defaultValue = "") String response, HttpSession session, RedirectAttributes flash) {
        try {
            reviews.submit(id, DemoSession.participantId(session), response);
            flash.addFlashAttribute("message", "Enviado al personal del evento. Aún no es una aprobación ni suma puntos.");
        } catch (BusinessRuleException error) {
            flash.addFlashAttribute("reviewError", error.getMessage());
            flash.addFlashAttribute("reviewDraft", response);
        }
        return "redirect:/matches/" + id + "#am-step";
    }

    @GetMapping("/events/{event}/reviews")
    public String list(@PathVariable long event, Model model) {
        model.addAttribute("event", events.get(event));
        model.addAttribute("items", reviews.forEvent(event));
        return "review/list";
    }

    @PostMapping("/events/{event}/reviews/{matchId}")
    public String decide(@PathVariable long event, @PathVariable long matchId, @RequestParam String decision,
                         @RequestParam(defaultValue = "") String comment, RedirectAttributes flash) {
        boolean approve = "approve".equals(decision);
        if (!approve && !"changes".equals(decision)) throw new BusinessRuleException("Elige aprobar o pedir corrección.");
        try {
            if (reviews.decide(event, matchId, approve, comment)) {
                try {
                    matches.completeAfterReview(matchId);
                    var outcome = reviews.outcome(event, matchId);
                    flash.addFlashAttribute("message", outcome.completed()
                        ? "Actividad aprobada. Se acreditaron +" + outcome.rewardPoints() + " puntos a cada persona, una sola vez."
                        : "Actividad aprobada. Confirmaciones: " + outcome.confirmations() + " de 2. Los puntos llegarán cuando ambas personas confirmen.");
                } catch (BusinessRuleException completion) {
                    flash.addFlashAttribute("message", "Actividad aprobada. La acreditación no terminó: " + completion.getMessage() + " Puedes volver a pulsar Aprobar; no duplica puntos.");
                }
            } else flash.addFlashAttribute("message", "Corrección solicitada. La pareja verá tu comentario y podrá reenviar.");
        } catch (BusinessRuleException error) {
            flash.addFlashAttribute("reviewError", error.getMessage());
            flash.addFlashAttribute("draftFor", matchId);
            flash.addFlashAttribute("draftComment", comment);
        }
        return "redirect:/events/" + event + "/reviews#review-" + matchId;
    }
}
