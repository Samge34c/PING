package com.ping.entry;

import com.ping.participant.*;
import com.ping.interest.*;
import com.ping.web.*;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@Controller @Profile("!bootstrap") @RequestMapping("/join")
public class ParticipantEntryController {
    private static final String DRAFT = "pingEntryDraft";
    private final ParticipantService participants;
    private final InterestService interests;
    private final ParticipantEntryService entry;
    private final com.ping.event.EventService events;
    public ParticipantEntryController(ParticipantService participants, InterestService interests, ParticipantEntryService entry, com.ping.event.EventService events) {
        this.participants = participants; this.interests = interests; this.entry = entry; this.events = events;
    }
    // The receipt and participant ID never come from request parameters.
    private static class Draft {
        final String key = UUID.randomUUID().toString();
        long revision; String alias = ""; Long eventId; List<Long> ids = List.of(); Long receipt;
    }
    private Draft draft(HttpSession session) {
        Draft draft = (Draft) session.getAttribute(DRAFT);
        if (draft == null) { draft = new Draft(); session.setAttribute(DRAFT, draft); }
        return draft;
    }
    private boolean entered(HttpSession session) { return session.getAttribute("pingDemoParticipantId") instanceof Long; }
    private String details(Draft d, Model model) {
        if (!model.containsAttribute("participantForm")) {
            ParticipantForm f = new ParticipantForm(); f.setAlias(d.alias); f.setEventId(d.eventId); model.addAttribute("participantForm", f);
        }
        if (d.eventId == null) return eventCode(d, model);
        var event = events.get(d.eventId); model.addAttribute("entryEvent", event); model.addAttribute("entryEventOpen", event.canStartNewMatches());
        model.addAttribute("draftKey", d.key); model.addAttribute("revision", d.revision);
        return "entry/details";
    }
    private String eventCode(Draft d, Model model) {
        model.addAttribute("draftKey", d.key); model.addAttribute("revision", d.revision); model.addAttribute("hasEntryEvent", d.eventId != null);
        return "entry/event";
    }
    private String chooseEvent(Draft d, String code, Model model, HttpServletResponse response) {
        try {
            var event = events.entryEvent(code);
            if (!event.getId().equals(d.eventId)) { d.eventId = event.getId(); d.revision++; }
            return "redirect:/join";
        } catch (BusinessRuleException | NotFoundException error) {
            response.setStatus(error instanceof NotFoundException ? 404 : 409);
            model.addAttribute("entryError", error.getMessage()); model.addAttribute("code", code); return eventCode(d, model);
        }
    }
    private String choices(Draft d, Model model) {
        model.addAttribute("alias", d.alias); model.addAttribute("eventName", events.get(d.eventId).getName());
        model.addAttribute("interests", interests.catalog()); model.addAttribute("selectedIds", d.ids);
        model.addAttribute("draftKey", d.key); model.addAttribute("revision", d.revision); return "entry/interests";
    }
    private boolean current(Draft d, String key, long revision) { return d.key.equals(key) && d.revision == revision; }
    @GetMapping public String start(@RequestParam(required=false) String code, HttpSession session, Model model, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, private");
        synchronized (session) {
            if (entered(session)) return "redirect:/me";
            Draft d = draft(session); if (code != null) return chooseEvent(d, code, model, response);
            return details(d, model);
        }
    }
    @GetMapping("/event") public String event(HttpSession session, Model model) {
        synchronized (session) { if (entered(session)) return "redirect:/me"; return eventCode(draft(session), model); }
    }
    @PostMapping("/event") public String event(@RequestParam String code, @RequestParam String draftKey, @RequestParam long revision,
            HttpSession session, Model model, HttpServletResponse response) {
        synchronized (session) {
            if (entered(session)) return "redirect:/me";
            Draft d = draft(session);
            if (!current(d, draftKey, revision)) { response.setStatus(409); model.addAttribute("entryError", "La entrada cambió en otra pestaña. Revisa tu borrador antes de cambiar el evento."); return eventCode(d, model); }
            return chooseEvent(d, code, model, response);
        }
    }
    @PostMapping public String details(@Valid @ModelAttribute("participantForm") ParticipantForm form, BindingResult errors,
            @RequestParam String draftKey, @RequestParam long revision, @RequestParam(defaultValue="interests") String action, HttpSession session, Model model) {
        synchronized (session) {
            if (entered(session)) return "redirect:/me";
            Draft d = draft(session);
            if (!current(d, draftKey, revision)) { model.asMap().remove("participantForm"); model.addAttribute("entryError", "Este paso cambió en otra pestaña. Revisa los datos guardados antes de continuar."); return details(d, model); }
            if (d.eventId == null) return "redirect:/join";
            if (!d.eventId.equals(form.getEventId())) { model.asMap().remove("participantForm"); model.addAttribute("entryError", "Usa Cambiar evento para identificar otro evento con su código."); return details(d, model); }
            d.alias = form.getAlias();
            if (action.equals("changeEvent")) { d.revision++; return "redirect:/join/event"; }
            if (!action.equals("interests")) { model.addAttribute("entryError", "Usa los botones de esta entrada para continuar."); return details(d, model); }
            if (!errors.hasFieldErrors("eventId") && participants.registrationEvents().stream().noneMatch(e -> e.getId().equals(d.eventId))) errors.rejectValue("eventId", "closed", "Elige un evento abierto para continuar.");
            if (errors.hasErrors()) return details(d, model);
            d.revision++; return "redirect:/join/interests";
        }
    }
    @GetMapping("/interests") public String interests(HttpSession session, Model model, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, private");
        synchronized (session) {
            if (entered(session)) return "redirect:/me";
            Draft d = draft(session); if (d.eventId == null || d.alias.isBlank()) return "redirect:/join";
            return choices(d, model);
        }
    }
    @PostMapping("/interests") public String finish(@RequestParam String draftKey, @RequestParam long revision,
            @RequestParam(defaultValue="complete") String action, @RequestParam(required=false) List<Long> interestIds,
            HttpSession session, Model model, HttpServletResponse response) {
        synchronized (session) {
            Draft d = draft(session);
            if (!d.key.equals(draftKey)) { response.setStatus(409); model.addAttribute("entryError", "La entrada cambió. Revisa tu borrador."); return details(d, model); }
            if (d.receipt != null) return "redirect:/me";
            if (entered(session)) return "redirect:/me";
            if (!current(d, draftKey, revision)) { response.setStatus(409); model.addAttribute("entryError", "La selección cambió en otra pestaña. Revisa los intereses y vuelve a continuar."); return choices(d, model); }
            if (interestIds != null && interestIds.stream().anyMatch(id -> id == null || id <= 0)) {
                response.setStatus(409); model.addAttribute("entryError", "Revisa los intereses seleccionados; tu borrador se conserva."); return choices(d, model);
            }
            d.ids = interestIds == null ? List.of() : List.copyOf(new LinkedHashSet<>(interestIds));
            if (action.equals("back")) { d.revision++; return "redirect:/join"; }
            if (!action.equals("complete")) { response.setStatus(400); model.addAttribute("entryError", "Usa los botones para continuar o corregir tu entrada."); return choices(d, model); }
            if (d.eventId == null || d.alias.isBlank()) return "redirect:/join";
            if (d.ids.stream().anyMatch(id -> id <= 0) || !interests.availableCatalog().stream().map(Interest::getId).toList().containsAll(d.ids)) {
                response.setStatus(409); model.addAttribute("entryError", "Uno de los intereses ya no está disponible. Revisa la selección; tu borrador se conserva."); return choices(d, model);
            }
            try {
                long id = entry.create(d.alias, d.eventId, d.ids);
                d.receipt = id; // Set only after the transaction committed; serializes double taps/retries.
                session.setAttribute("pingDemoParticipantId", id); session.setAttribute("pingEntryMode", "participant");
                return "redirect:/me";
            } catch (BusinessRuleException | NotFoundException error) {
                response.setStatus(409); model.addAttribute("entryError", "No pudimos guardar la entrada. Revisa el evento y los intereses; tu borrador se conserva."); return choices(d, model);
            } catch (org.springframework.dao.DataAccessException error) {
                response.setStatus(503); model.addAttribute("entryError", "No pudimos guardar ahora. Tu borrador se conserva; vuelve a intentarlo."); return choices(d, model);
            }
        }
    }
}
