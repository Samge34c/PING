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
        var e=events.lock(event);requireLegacyImport(e);var p=id==null?new MeetingPoint(event):point(event,id);
        if(id==null && points(event).stream().filter(x->!x.isArchived()).count()>=3)throw new BusinessRuleException("Máximo3 puntos. Archiva uno para añadir otro.");
        if(p.isArchived())throw new BusinessRuleException("El punto está archivado.");if(id!=null)revision(form.getVersion(),p.getVersion());
        p.change(form);e.enableStructuredEncounter();return points.save(p).getId();
    }
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public long saveActivity(long event,Long id,@Valid ActivityForm form){
        var e=events.lock(event);requireLegacyImport(e);var a=id==null?new EventActivity(event):activity(event,id);
        if(id==null && activities(event).stream().filter(x->!x.isArchived()).count()>=10)throw new BusinessRuleException("Máximo10 actividades. Archiva una para añadir otra.");
        if(a.isArchived())throw new BusinessRuleException("La actividad está archivada.");if(id!=null)revision(form.getVersion(),a.getVersion());
        if(points.findAllById(form.getPointIds()).size()!=form.getPointIds().size() || points.findAllById(form.getPointIds()).stream().anyMatch(p->p.getEventId()!=event))throw new BusinessRuleException("Elige puntos de este evento.");
        if(interests.findAllById(form.getInterestIds()).size()!=form.getInterestIds().size())throw new BusinessRuleException("Elige intereses del catálogo.");
        catalog.validateSelection(form.getInterestIds(),a.getInterestIds());
        if(form.isActive() && points.findAllById(form.getPointIds()).stream().noneMatch(p->p.isActive()&&!p.isArchived()))throw new BusinessRuleException("Una actividad activa necesita un punto activo compatible.");
        a.change(form);e.enableStructuredEncounter();return activities.save(a).getId();
    }
    /** Explicit, idempotent import preserves original V14 lists as a recoverable source. */
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void importLegacy(long event){
        var e=events.lock(event);if(e.isStructuredEncounter())return;
        var names=EncounterOptions.lines(e.getMeetingPoints());if(names.isEmpty())names=List.of(e.getMeetingPoint());
        var rows=EncounterOptions.lines(e.getEncounterActivities());if(rows.isEmpty())rows=List.of(ActivityCatalog.activity(""));
        // Preflight before mutation: over-limit legacy data remains intact and editable.
        if(new LinkedHashSet<>(names).size()>3 || new LinkedHashSet<>(rows).size()>10)throw new BusinessRuleException("El catálogo anterior supera3 puntos o10 actividades. Reduce las listas antes de importar; no se borró nada.");
        e.enableStructuredEncounter();
        Set<Long> ids=new LinkedHashSet<>();
        for(String name:new LinkedHashSet<>(names)){var f=new PointForm();f.setName(name);f.setDirections("Busca el punto público «"+name+"». Si no lo identificas, consulta al organizador.");ids.add(savePoint(event,null,f));}
        for(String text:new LinkedHashSet<>(rows)){var f=new ActivityForm();f.setTitle(text.substring(0,Math.min(120,text.length())));f.setInstructions(text);f.setPointIds(ids);saveActivity(event,null,f);}
    }
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void archivePoint(long event,long id){events.lock(event);var p=point(event,id);if(activities(event).stream().anyMatch(a->a.isActive()&&!a.isArchived()&&a.getPointIds().contains(id)&&points(event).stream().noneMatch(x->x.isActive()&&!x.isArchived()&&x.getId()!=id&&a.getPointIds().contains(x.getId()))))throw new BusinessRuleException("Asigna otro punto activo a sus actividades antes de archivarlo.");p.archive();}
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void archiveActivity(long event,long id){events.lock(event);activity(event,id).archive();}
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void words(long event,String value,long version){var e=events.lock(event);revision(version,e.getVersion());value=value.replace(",","\n");EncounterOptions.checkGrowth(value,e.getEncounterWords(),100,"Palabras");e.changeWords(EncounterOptions.normalize(value,40,"Palabras"));}
    private void requireLegacyImport(Event e){if(!e.isStructuredEncounter()&&(!EncounterOptions.unique(e.getMeetingPoints()).isEmpty()||!EncounterOptions.unique(e.getEncounterActivities()).isEmpty()))throw new BusinessRuleException("Importa primero el catálogo anterior. Puedes reducir sus listas sin borrar encuentros activos.");}
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void legacy(long event,String names,String tasks,long version){var e=events.lock(event);revision(version,e.getVersion());if(e.isStructuredEncounter())throw new BusinessRuleException("El catálogo ya se importó. Usa las filas editables.");if(names.length()>4000||tasks.length()>12000)throw new BusinessRuleException("Las listas superan el tamaño permitido.");EncounterOptions.checkGrowth(names,e.getMeetingPoints(),3,"Puntos");EncounterOptions.checkGrowth(tasks,e.getEncounterActivities(),10,"Actividades");var f=EventForm.from(e);f.setMeetingPoints(EncounterOptions.normalize(names,160,"Puntos"));f.setEncounterActivities(EncounterOptions.normalize(tasks,500,"Actividades"));events.update(event,f);}
    public record Assignment(ActivityCatalog.Encounter encounter,String directions,String title,int rewardPoints,Long pointId){}
    public Assignment assign(Event event,String common,long matchId){
        if((!event.isStructuredEncounter() && (EncounterOptions.unique(event.getMeetingPoints()).size()>3 || EncounterOptions.unique(event.getEncounterActivities()).size()>10)) || EncounterOptions.unique(event.getEncounterWords()).size()>100 || (event.isStructuredEncounter() && (points(event.getId()).stream().filter(p->!p.isArchived()).count()>3 || activities(event.getId()).stream().filter(a->!a.isArchived()).count()>10)))throw new EncounterUnavailableException("La configuración anterior supera los límites. Pide al organizador reducirla; la propuesta y los encuentros anteriores se conservan.");
        if(!event.isStructuredEncounter())return new Assignment(ActivityCatalog.encounter(common,matchId,event),"","",10,null);
        var available=points(event.getId()).stream().filter(p->p.isActive()&&!p.isArchived()).toList();
        var names=new HashSet<>(Arrays.asList(common.split(" · ")));
        var interestNames=new HashMap<Long,String>();interests.findAll().forEach(i->interestNames.put(i.getId(),i.getName()));
        var candidates=activities(event.getId()).stream().filter(a->a.isActive()&&!a.isArchived())
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
