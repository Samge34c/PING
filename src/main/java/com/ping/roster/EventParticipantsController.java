package com.ping.roster;

import com.ping.event.EventService;
import com.ping.web.BusinessRuleException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** DEC-068. «Participantes» of the selected event. AccessPolicy requires event permission; the service requires the ORGANIZER role. */
@Controller @Profile("!bootstrap")
public class EventParticipantsController {
    private final EventParticipantsService roster; private final EventService events;
    public EventParticipantsController(EventParticipantsService roster, EventService events) { this.roster = roster; this.events = events; }

    @GetMapping("/events/{id}/participants")
    public String list(@PathVariable long id, @RequestParam(name = "q", required = false) String q, Model m) {
        var rows = roster.search(id, q);
        m.addAttribute("event", events.get(id)); m.addAttribute("rows", rows); m.addAttribute("q", q == null ? "" : q.strip());
        m.addAttribute("total", roster.count(id)); m.addAttribute("limited", rows.size() >= EventParticipantsService.MAX_ROWS);
        return "events/participants";
    }
    @GetMapping("/events/{id}/participants/{participant}/withdraw")
    public String confirm(@PathVariable long id, @PathVariable long participant, Model m) {
        m.addAttribute("event", events.get(id)); m.addAttribute("person", roster.detail(id, participant));
        return "events/participant-withdraw";
    }
    @PostMapping("/events/{id}/participants/{participant}/withdraw")
    public String withdraw(@PathVariable long id, @PathVariable long participant, @RequestParam(defaultValue = "") String reason, Model m, RedirectAttributes flash) {
        try {
            var done = roster.withdraw(id, participant, reason);
            flash.addFlashAttribute("rosterMessage", "Retiraste a «" + done.alias() + "» de «" + events.get(id).getName() + "»."
                    + (done.encounters() > 0 ? " Su encuentro abierto se canceló y se avisó a la otra persona, sin el motivo." : "")
                    + (done.claims() > 0 ? " " + done.claims() + (done.claims() == 1 ? " canje sin entregar se anuló" : " canjes sin entregar se anularon") + " y sus puntos volvieron a su saldo." : ""));
            return "redirect:/events/" + id + "/participants?q=" + java.net.URLEncoder.encode(done.alias(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (BusinessRuleException e) {
            m.addAttribute("event", events.get(id)); m.addAttribute("person", roster.detail(id, participant));
            m.addAttribute("withdrawError", e.getMessage()); m.addAttribute("reason", reason);
            return "events/participant-withdraw";
        }
    }
    @PostMapping("/events/{id}/participants/{participant}/readmit")
    public String readmit(@PathVariable long id, @PathVariable long participant, RedirectAttributes flash) {
        String alias = roster.readmit(id, participant);
        flash.addFlashAttribute("rosterMessage", "Readmitiste a «" + alias + "» en «" + events.get(id).getName() + "». Puede volver a entrar con su cuenta y marcar su disponibilidad.");
        return "redirect:/events/" + id + "/participants?q=" + java.net.URLEncoder.encode(alias, java.nio.charset.StandardCharsets.UTF_8);
    }
}
