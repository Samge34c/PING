package com.ping.floorplan;

import com.ping.event.*;
import com.ping.experience.EventExperienceCatalog;
import com.ping.match.*;
import com.ping.participant.ParticipantService;
import com.ping.web.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller @Profile("!bootstrap")
public class FloorplanViewController {
    private final FloorplanService plans;
    private final EventService events;
    private final ParticipantService people;
    private final MatchService matches;
    public FloorplanViewController(FloorplanService plans,EventService events,ParticipantService people,MatchService matches){this.plans=plans;this.events=events;this.people=people;this.matches=matches;}

    @GetMapping("/events/{id}/plan")
    public String event(@PathVariable long id,@RequestParam(required=false) Long pointId,HttpSession session,Model model){
        // Mi evento resolves an ongoing mission through its snapshot, including after replacement.
        if(session.getAttribute("pingDemoParticipantId") instanceof Long self && people.get(self).eventId()==id){
            var active=matches.list(self).stream().filter(m->m.status()==MatchStatus.ACTIVE).findFirst().orElse(null);
            if(active!=null)return renderMatch(active,events.get(id),model);
        }
        var event=events.get(id);var plan=plans.current(id);var points=plans.points(id);
        var selected=pointId==null ? points.stream().filter(FloorplanService.Point::active).findFirst().orElse(points.isEmpty()?null:points.getFirst()) :
            points.stream().filter(p->p.id()==pointId).findFirst().orElseThrow(()->new NotFoundException("No encontramos ese punto en este evento."));
        model.addAttribute("event",event);model.addAttribute("plan",plan);model.addAttribute("planPoints",points);model.addAttribute("selectedPointId",selected==null?null:selected.id());
        model.addAttribute("pointName",selected==null?event.getMeetingPoint():selected.name());
        model.addAttribute("directions",selected==null?"Busca el punto público indicado. Si no lo identificas, consulta al organizador.":selected.directions());
        model.addAttribute("pointActive",selected==null||selected.active());model.addAttribute("assignment",false);
        model.addAttribute("fpX",selected==null?null:selected.x());model.addAttribute("fpY",selected==null?null:selected.y());model.addAttribute("fpLabel","1 · Punto seleccionado");
        model.addAttribute("backUrl",session.getAttribute("pingDemoParticipantId") instanceof Long?"/me":"/events/"+id);
        model.addAttribute("backLabel",session.getAttribute("pingDemoParticipantId") instanceof Long?"Volver a Mi Ping":"Volver al evento");
        return "floorplan/participant";
    }
    @GetMapping("/matches/{id}/plan")
    public String match(@PathVariable long id,HttpSession session,Model model){
        long self=DemoSession.participantId(session);var match=matches.get(id,self);
        return renderMatch(match,events.get(people.get(self).eventId()),model);
    }
    private String renderMatch(MatchView match,Event event,Model model){
        if(match.status()!=MatchStatus.ACTIVE)throw new BusinessRuleException("El punto asignado se muestra durante el encuentro activo, tras aceptar ambos.");
        if(match.floorplanId()==null){
            var experience=EventExperienceCatalog.forEvent(event.getName());
            var zone=experience==null?null:experience.atPoint(match.meetingPoint());
            if(zone!=null)return "redirect:/events/"+event.getId()+"/map?zone="+zone.id()+"&matchId="+match.id();
        }
        model.addAttribute("event",event);model.addAttribute("assignment",true);model.addAttribute("pointActive",true);
        model.addAttribute("plan",match.floorplanId()==null?null:plans.plan(event.getId(),match.floorplanId()));
        model.addAttribute("pointName",match.meetingPoint());model.addAttribute("directions",match.meetingDirections()==null||match.meetingDirections().isBlank()?"Busca el punto público indicado. Si no lo identificas, consulta al organizador.":match.meetingDirections());
        model.addAttribute("fpX",match.floorplanX());model.addAttribute("fpY",match.floorplanY());model.addAttribute("fpLabel","1 · Tu punto de encuentro");
        model.addAttribute("backUrl","/matches/"+match.id());model.addAttribute("backLabel","Volver al encuentro");
        return "floorplan/participant";
    }
}
