package com.ping.encounter;

import java.util.*;
import com.ping.event.*;
import com.ping.match.ActivityCatalog;
import com.ping.interest.InterestRepository;
import com.ping.web.*;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service @Profile("!bootstrap") @Validated @Transactional(readOnly=true)
public class EncounterService {
    private final MeetingPointRepository points;
    private final EventActivityRepository activities;
    private final EventService events;
    private final InterestRepository interests;
    private final com.ping.interest.InterestCatalogService catalog;
    public EncounterService(MeetingPointRepository points,EventActivityRepository activities,EventService events,InterestRepository interests,com.ping.interest.InterestCatalogService catalog){this.catalog=catalog;this.points=points;this.activities=activities;this.events=events;this.interests=interests;}
    public List<MeetingPoint> points(long event){events.get(event);return points.findByEventIdOrderByIdAsc(event);}
    public List<EventActivity> activities(long event){events.get(event);return activities.findByEventIdOrderByIdAsc(event);}
    public PointForm pointForm(long event,long id){return PointForm.from(point(event,id));}
    public ActivityForm activityForm(long event,long id){return ActivityForm.from(activity(event,id));}
    private MeetingPoint point(long event,long id){var p=points.findById(id).orElseThrow(()->new NotFoundException("No encontramos ese punto."));if(p.getEventId()!=event)throw new NotFoundException("El punto pertenece a otro evento.");return p;}
    private EventActivity activity(long event,long id){var a=activities.findById(id).orElseThrow(()->new NotFoundException("No encontramos esa actividad."));if(a.getEventId()!=event)throw new NotFoundException("La actividad pertenece a otro evento.");return a;}
    private void revision(Long expected,long current){if(expected==null || expected!=current)throw new BusinessRuleException("La configuración cambió. Vuelve a abrir el editor.");}
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public long savePoint(long event,Long id,@Valid PointForm form){
        var e=events.lock(event);var p=id==null?new MeetingPoint(event):point(event,id);if(id!=null)revision(form.getVersion(),p.getVersion());
        p.change(form);e.enableStructuredEncounter();return points.save(p).getId();
    }
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public long saveActivity(long event,Long id,@Valid ActivityForm form){
        var e=events.lock(event);var a=id==null?new EventActivity(event):activity(event,id);if(id!=null)revision(form.getVersion(),a.getVersion());
        if(points.findAllById(form.getPointIds()).size()!=form.getPointIds().size() || points.findAllById(form.getPointIds()).stream().anyMatch(p->p.getEventId()!=event))throw new BusinessRuleException("Elige puntos de este evento.");
        if(interests.findAllById(form.getInterestIds()).size()!=form.getInterestIds().size())throw new BusinessRuleException("Elige intereses del catálogo.");
        catalog.validateSelection(form.getInterestIds(),a.getInterestIds());
        a.change(form);e.enableStructuredEncounter();return activities.save(a).getId();
    }
    /** Explicit, idempotent import preserves original V14 lists as a recoverable source. */
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void importLegacy(long event){
        var e=events.lock(event);if(e.isStructuredEncounter())return;
        var names=EncounterOptions.lines(e.getMeetingPoints());if(names.isEmpty())names=List.of(e.getMeetingPoint());
        Set<Long> ids=new LinkedHashSet<>();
        for(String name:new LinkedHashSet<>(names)){var f=new PointForm();f.setName(name);f.setDirections("Busca el punto público «"+name+"». Si no lo identificas, consulta al organizador.");ids.add(savePoint(event,null,f));}
        var rows=EncounterOptions.lines(e.getEncounterActivities());if(rows.isEmpty())rows=List.of(ActivityCatalog.activity(""));
        for(String text:new LinkedHashSet<>(rows)){var f=new ActivityForm();f.setTitle(text.substring(0,Math.min(120,text.length())));f.setInstructions(text);f.setPointIds(ids);saveActivity(event,null,f);}
    }
    public record Assignment(ActivityCatalog.Encounter encounter,String directions,String title,int rewardPoints,Long pointId){}
    public Assignment assign(Event event,String common,long matchId){
        if(!event.isStructuredEncounter())return new Assignment(ActivityCatalog.encounter(common,matchId,event),"","",10,null);
        var available=points(event.getId()).stream().filter(MeetingPoint::isActive).toList();
        var names=new HashSet<>(Arrays.asList(common.split(" · ")));
        var interestNames=new HashMap<Long,String>();interests.findAll().forEach(i->interestNames.put(i.getId(),i.getName()));
        var candidates=activities(event.getId()).stream().filter(EventActivity::isActive)
            .filter(a->a.getInterestIds().isEmpty() || a.getInterestIds().stream().anyMatch(i->names.contains(interestNames.get(i))))
            .filter(a->available.stream().anyMatch(p->a.getPointIds().contains(p.getId())))
            .sorted(Comparator.<EventActivity>comparingLong(a->a.getInterestIds().stream().filter(i->names.contains(interestNames.get(i))).count()).reversed().thenComparing(EventActivity::getId)).toList();
        if(candidates.isEmpty())throw new EncounterUnavailableException("No hay una actividad y un punto activos compatibles. La propuesta se conserva: pide al organizador revisar la configuración y reintenta aceptar, o cancela desde Conectar.");
        var a=candidates.getFirst();var compatible=available.stream().filter(p->a.getPointIds().contains(p.getId())).toList();
        var p=compatible.get((int)Math.floorMod(matchId-1,compatible.size()));
        String fallback=ActivityCatalog.word(common,matchId);
        String base=EncounterOptions.select(event.getEncounterWords(),matchId,null);
        return new Assignment(new ActivityCatalog.Encounter(p.getName(),base==null?fallback:base+"-"+Long.toString(matchId,36).toUpperCase(Locale.ROOT),a.getInstructions()),p.getDirections(),a.getTitle(),a.getRewardPoints(),p.getId());
    }
}
