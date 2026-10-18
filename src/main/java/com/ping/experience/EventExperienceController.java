package com.ping.experience;

import com.ping.event.EventService;
import com.ping.match.*;
import com.ping.participant.ParticipantService;
import com.ping.web.NotFoundException;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller @Profile("!bootstrap")
public class EventExperienceController {
    private final EventService events;
    private final ParticipantService participants;
    private final MatchService matches;
    public EventExperienceController(EventService events, ParticipantService participants, MatchService matches) {
        this.events = events; this.participants = participants; this.matches = matches;
    }
    private EventExperienceCatalog.Experience experience(long id, Model model) {
        var event = events.get(id); var experience = EventExperienceCatalog.forEvent(event.getName());
        if (experience == null) throw new NotFoundException("Este evento todavía no tiene una guía de zonas preparada. Vuelve al detalle del evento.");
        model.addAttribute("event", event); model.addAttribute("experience", experience); return experience;
    }
    @GetMapping("/me/event") public String myEvent(HttpSession session) {
        if (!(session.getAttribute("pingDemoParticipantId") instanceof Long self)) return "redirect:/events";
        var p = participants.get(self);
        return "redirect:/events/" + p.eventId() + (EventExperienceCatalog.forEvent(p.eventName()) != null ? "/hub" : "/plan");
    }
    @GetMapping("/events/{id}/hub") public String hub(@PathVariable long id, HttpSession session, Model model) {
        experience(id, model);
        if (session.getAttribute("pingDemoParticipantId") instanceof Long self) {
            var participant = participants.get(self);
            if (participant.eventId() == id) {
                model.addAttribute("participant", participant);
                var history = matches.list(self);
                model.addAttribute("currentMatch", history.stream().filter(m -> m.status() == MatchStatus.OFFERED || m.status() == MatchStatus.ACTIVE).findFirst().orElse(null));
                model.addAttribute("badges", EventExperienceCatalog.badges(history));
            }
        }
        return "experience/hub";
    }
    @GetMapping("/events/{id}/map") public String map(@PathVariable long id, @RequestParam(required = false) String zone,
            @RequestParam(required = false) Long matchId, HttpSession session, Model model) {
        if(matchId==null && events.get(id).getFloorplanId()!=null)return "redirect:/events/"+id+"/plan";
        if(matchId!=null){long self=DemoSession.participantId(session);if(participants.get(self).eventId()!=id)throw new NotFoundException("Esta conexión pertenece a otro evento.");
            var active=matches.get(matchId,self);if(active.status()==MatchStatus.ACTIVE && active.floorplanId()!=null)return "redirect:/matches/"+matchId+"/plan";
        }
        var experience = experience(id, model);
        var selected = zone == null ? experience.zones().getFirst() : experience.zone(zone);
        if (selected == null) throw new NotFoundException("No encontramos esa zona. Vuelve al mapa del evento.");
        String back = "/events/" + id + "/hub";
        if (matchId != null) {
            long self = DemoSession.participantId(session);
            if (participants.get(self).eventId() != id) throw new NotFoundException("Esta conexión pertenece a otro evento.");
            matches.get(matchId, self); back = "/matches/" + matchId;
        }
        model.addAttribute("selectedZone", selected); model.addAttribute("backUrl", back);
        model.addAttribute("fromMatch", matchId != null); model.addAttribute("matchId", matchId);
        return "experience/map";
    }
}
