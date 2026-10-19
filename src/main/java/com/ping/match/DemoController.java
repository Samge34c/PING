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
    private final com.ping.moderation.ModerationService moderation;
    private final com.ping.experience.DemoRecognitionHints recognition;
    private final RecognitionHintService hints;
    private final ParticipantService participants;
    private final MatchingService matching;
    private final MatchService matches;
    private final com.ping.event.EventService events;
    private final com.ping.verification.CredentialService credentials;
    private final com.ping.auth.AuthSupport auth;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    public DemoController(ParticipantService participants, MatchingService matching, MatchService matches, com.ping.event.EventService events, com.ping.moderation.ModerationService moderation, com.ping.experience.DemoRecognitionHints recognition, RecognitionHintService hints,com.ping.auth.AuthSupport auth,org.springframework.jdbc.core.JdbcTemplate jdbc,com.ping.verification.CredentialService credentials) { this.credentials=credentials;this.auth=auth;this.jdbc=jdbc;
        this.moderation = moderation; this.recognition = recognition; this.hints = hints;
        this.participants = participants; this.matching = matching; this.matches = matches; this.events = events;
    }
    @GetMapping("/demo") public String select(@RequestParam(required = false) Long participantId, Model model) { model.addAttribute("selectedParticipantId", participantId); model.addAttribute("participants", participants.list().stream().filter(p->auth.legacyFixture()||jdbc.queryForObject("select count(*) from demo_scenarios d where d.event_id=?",Integer.class,p.eventId())>0&&jdbc.queryForObject("select count(*) from account_participations where participant_id=?",Integer.class,p.id())==0).toList()); return "demo/select"; }
    @PostMapping("/demo") public String enter(@RequestParam long participantId, HttpSession session) {
        participants.get(participantId); session.removeAttribute("pingEntryDraft"); session.removeAttribute("pingEntryMode"); session.setAttribute(SELECTED, participantId);session.setAttribute("pingEntryMode","demo"); return "redirect:/me";
    }
    @PostMapping("/demo/exit") public String exit(HttpSession session) { boolean ownEntry = "participant".equals(session.getAttribute("pingEntryMode")); session.removeAttribute(SELECTED); session.removeAttribute("pingEntryMode"); session.removeAttribute("pingEntryDraft"); return ownEntry ? "redirect:/join" : "redirect:/demo"; }
    private long self(HttpSession session) {
        return DemoSession.participantId(session);
    }
    private MatchView current(java.util.List<MatchView> history) {
        return history.stream().filter(m -> m.status() == MatchStatus.OFFERED || m.status() == MatchStatus.ACTIVE).findFirst().orElse(null);
    }
    private String signature(ParticipantView p, com.ping.event.EventStatus status, MatchView m) {
        return p.id() + ":" + p.version() + ":" + status.name() + ":" + (m == null ? "waiting" : m.id() + ":" + m.status().name() + ":" + m.accepted() + ":" + m.otherAccepted() + ":" + m.confirmed() + ":" + m.otherConfirmed() + ":" + m.completionPending() + ":" + m.arrived() + ":" + m.otherArrived()+":"+m.verified()+":"+m.otherVerified()+":"+ownCodeRevision(p.id(),m));
    }
    private long ownCodeRevision(long self,MatchView m){return m.status()==MatchStatus.ACTIVE&&m.verificationPolicy()>=2?jdbc.queryForObject("select count(*) from purpose_credentials where purpose='ENCOUNTER' and context_id=? and issuer_id=?",Long.class,m.id(),self):0;}
    @GetMapping("/me/state") @ResponseBody
    public java.util.Map<String, String> state(HttpSession session, jakarta.servlet.http.HttpServletResponse response) {
        long id = self(session); var p = participants.get(id); response.setHeader("Cache-Control", "no-store");
        var current = current(matches.list(id));
        var state = new java.util.LinkedHashMap<String, String>();
        state.put("signature", signature(p, events.get(p.eventId()).getStatus(), current));
        state.put("matchStatus", current == null ? "NONE" : current.status().name());
        if (current != null && current.status() == MatchStatus.ACTIVE) {
            var hint = hints.other(current.id(), id);
            if (hint != null) state.put("recognitionRevision", hint.revision());
        }
        return state;
    }
    @GetMapping("/me") public String dashboard(HttpSession session, Model model) {
        if (session.getAttribute(SELECTED) == null) return "redirect:/join";
        long id = self(session); var history = matches.list(id);
        model.addAttribute("recognitionHintForm", hints.ownForm(id));
        var participant = participants.get(id); model.addAttribute("participant", participant); model.addAttribute("matches", history);
        model.addAttribute("event", events.get(participant.eventId()));
        model.addAttribute("currentMatch", current(history));
        var experience = com.ping.experience.EventExperienceCatalog.forEvent(participant.eventName());
        model.addAttribute("experience", experience);
        if (experience != null) model.addAttribute("badges", com.ping.experience.EventExperienceCatalog.badges(history));
        model.addAttribute("lastCompletion", !history.isEmpty() && history.getFirst().status() == MatchStatus.COMPLETED ? history.getFirst() : null);
        model.addAttribute("liveSignature", signature(participant, events.get(participant.eventId()).getStatus(), current(history)));
        model.addAttribute("held", history.stream().anyMatch(m -> m.status() == MatchStatus.OFFERED || m.status() == MatchStatus.ACTIVE));
        model.addAttribute("completedCount", history.stream().filter(m -> m.status() == MatchStatus.COMPLETED).count()); return "demo/dashboard";
    }
    @GetMapping("/connect") public String connect(HttpSession session, Model model) {
        String page = dashboard(session, model);
        if (page.startsWith("redirect:")) return page;
        MatchView match = (MatchView) model.getAttribute("currentMatch");
        return match == null ? "demo/connect" : "redirect:/matches/" + match.id();
    }
    @PostMapping("/me/availability") public String availability(@ModelAttribute AvailabilityForm form, @RequestParam(defaultValue="me") String returnTo, HttpSession session) {
        participants.setAvailability(self(session), form); return "connect".equals(returnTo) ? "redirect:/connect" : "redirect:/me";
    }
    @PostMapping("/me/search") public String search(@RequestParam(defaultValue="me") String returnTo, HttpSession session, RedirectAttributes flash) {
        long id = self(session);
        try { return "redirect:/matches/" + matching.search(id); }
        catch (BusinessRuleException error) { flash.addFlashAttribute("message", error.getMessage()); flash.addFlashAttribute("messageTone", "notice"); return "connect".equals(returnTo) ? "redirect:/connect" : "redirect:/me"; }
    }
    @GetMapping("/matches/{id}") public String match(@PathVariable long id, HttpSession session, Model model) {
        long selected = self(session); var participant = participants.get(selected); model.addAttribute("participant", participant); model.addAttribute("event", events.get(participant.eventId()));
        model.addAttribute("moderation", moderation.status(id, selected));
        model.addAttribute("reportReasons", com.ping.moderation.ReportReason.values());
        var match = matches.get(id, selected); model.addAttribute("match", match);
        model.addAttribute("credentialRequestKey",java.util.UUID.randomUUID().toString());
        var personalCode=com.ping.verification.CredentialSession.get(session,"ENCOUNTER",id,selected);
        model.addAttribute("myCredential",personalCode!=null&&credentials.valid(personalCode)&&!match.otherVerified()?personalCode:null);
        model.addAttribute("recognitionHintForm", hints.ownForm(selected));
        if (match.status() == MatchStatus.ACTIVE) { var authorized = hints.other(id, selected); if (authorized != null) model.addAttribute("recognitionRevision", authorized.revision()); }
        var experience = com.ping.experience.EventExperienceCatalog.forEvent(participant.eventName());
        model.addAttribute("experience", experience);
        model.addAttribute("recognitionHint", recognition.forMatch(participant.eventId(), selected, match));
        if (experience != null && match.status() == MatchStatus.COMPLETED) {
            var history = matches.list(selected);
            model.addAttribute("badges", com.ping.experience.EventExperienceCatalog.badges(history));
            model.addAttribute("promoUnlocked", com.ping.experience.EventExperienceCatalog.promoUnlocked(history));
        }
        if (match.status() == MatchStatus.OFFERED || match.status() == MatchStatus.ACTIVE) model.addAttribute("liveSignature", signature(participant, events.get(participant.eventId()).getStatus(), current(matches.list(selected)))); return "demo/match";
    }
    @PostMapping("/matches/{id}/accept") public String accept(@PathVariable long id, HttpSession session, RedirectAttributes flash) {
        try { matches.accept(id, self(session)); }
        catch (com.ping.encounter.EncounterUnavailableException error) { flash.addFlashAttribute("message",error.getMessage());flash.addFlashAttribute("messageTone","notice"); }
        return "redirect:/matches/" + id;
    }
    @PostMapping("/matches/{id}/reject") public String reject(@PathVariable long id, HttpSession session) { matches.cancel(id, self(session)); return "redirect:/matches/" + id; }
    @PostMapping("/matches/{id}/cancel") public String cancel(@PathVariable long id, HttpSession session) { matches.cancel(id, self(session)); return "redirect:/matches/" + id; }
    @PostMapping("/matches/{id}/arrive") public String arrive(@PathVariable long id, HttpSession session) { matches.arrive(id, self(session)); return "redirect:/matches/" + id + "#am-arrival-title"; }
    @PostMapping("/matches/{id}/confirm") public String confirm(@PathVariable long id, HttpSession session) { matches.confirm(id, self(session)); return "redirect:/matches/" + id + "#am-confirm-title"; }
    @PostMapping("/matches/{id}/block") public String block(@PathVariable long id, HttpSession session, RedirectAttributes flash) {
        moderation.block(id, self(session)); flash.addFlashAttribute("message", "Bloqueo guardado. No recibirán nuevas propuestas entre sí. La conexión actual conserva su estado."); return "redirect:/matches/" + id;
    }
    @PostMapping("/matches/{id}/report") public String report(@PathVariable long id, @RequestParam com.ping.moderation.ReportReason reason,
            @RequestParam(defaultValue = "") String comment, HttpSession session, RedirectAttributes flash) {
        moderation.report(id, self(session), reason, comment); flash.addFlashAttribute("message", "Reporte guardado para revisión administrativa. No aplica sanciones ni bloqueos automáticos."); return "redirect:/matches/" + id;
    }
    @PostMapping("/matches/{id}/feedback") public String feedback(@PathVariable long id, @RequestParam String rating, HttpSession session) {
        Feedback.Rating selected;
        try { selected = Feedback.Rating.valueOf(rating); }
        catch (IllegalArgumentException error) { throw new BusinessRuleException("Selecciona una opción válida de feedback."); }
        matches.giveFeedback(id, self(session), selected); return "redirect:/matches/" + id;
    }
}
