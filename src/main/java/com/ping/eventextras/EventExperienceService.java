package com.ping.eventextras;

import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import com.ping.auth.AuthSupport;
import com.ping.event.*;
import com.ping.encounter.*;
import com.ping.floorplan.FloorplanService;
import com.ping.participant.*;
import com.ping.reward.RewardService;
import com.ping.web.*;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.validation.annotation.Validated;

@Service @Profile("!bootstrap") @Validated @Transactional(readOnly=true)
public class EventExperienceService {
 private final JdbcTemplate sql; private final EventService events; private final AuthSupport auth;
 private final EncounterService encounters; private final FloorplanService plans; private final RewardService rewards;
 private final ParticipantService participants;
 private final EventEntryLinks links;
 public EventExperienceService(JdbcTemplate s,EventService e,AuthSupport a,EncounterService c,FloorplanService p,RewardService r,ParticipantService people,EventEntryLinks l){sql=s;events=e;auth=a;encounters=c;plans=p;rewards=r;participants=people;links=l;}
 private void manage(long event){if(!auth.legacyFixture())auth.manage(event);}
 public record Settings(String zoneId,String staffLocation,String officialContact,boolean challengeEnabled,long version){}
 public Settings settings(long event){events.get(event);return sql.query("select * from event_experience_settings where event_id=?",(r,n)->new Settings(r.getString("zone_id"),r.getString("staff_location"),r.getString("official_contact"),r.getBoolean("challenge_enabled"),r.getLong("version")),event).stream().findFirst().orElse(new Settings("America/Bogota","","",false,0));}
 public ExperienceSettingsForm settingsForm(long event){manage(event);var s=settings(event);var f=new ExperienceSettingsForm();f.setVersion(s.version());f.setZoneId(s.zoneId());f.setStaffLocation(s.staffLocation());f.setOfficialContact(s.officialContact());f.setChallengeEnabled(s.challengeEnabled());return f;}
 public static ZoneId zone(String value){try{if(!ZoneId.getAvailableZoneIds().contains(value))throw new IllegalArgumentException();return ZoneId.of(value);}catch(RuntimeException ex){throw new BusinessRuleException("Elige una zona horaria IANA válida, por ejemplo America/Bogota.");}}
 @Transactional(isolation=Isolation.READ_COMMITTED)
 public void saveSettings(long event,@Valid ExperienceSettingsForm f){manage(event);events.lock(event);zone(f.getZoneId());var s=settings(event);revision(f.getVersion(),s.version());
  if(sql.queryForObject("select count(*) from event_experience_settings where event_id=?",Integer.class,event)==0)sql.update("insert into event_experience_settings(event_id,zone_id,staff_location,official_contact,challenge_enabled,version) values(?,?,?,?,?,1)",event,f.getZoneId(),f.getStaffLocation().strip(),f.getOfficialContact().strip(),f.isChallengeEnabled());
  else sql.update("update event_experience_settings set zone_id=?,staff_location=?,official_contact=?,challenge_enabled=?,version=version+1 where event_id=?",f.getZoneId(),f.getStaffLocation().strip(),f.getOfficialContact().strip(),f.isChallengeEnabled(),event);
 }
 private static void revision(Long expected,long actual){if(expected==null||expected!=actual)throw new BusinessRuleException("La información cambió. Conservamos tu borrador; recarga antes de guardar de nuevo.");}
 public record Agenda(long id,long eventId,String title,String description,Instant startsAt,Instant endsAt,String zoneId,String place,Long pointId,boolean active,long version,String requestKey){
  public String day(){return DateTimeFormatter.ofPattern("EEE d MMM yyyy",Locale.forLanguageTag("es-CO")).format(startsAt.atZone(ZoneId.of(zoneId)));}
  public String hour(){return DateTimeFormatter.ofPattern("HH:mm").format(startsAt.atZone(ZoneId.of(zoneId)));}
  public String state(Instant now){if(now.isBefore(startsAt))return "Próxima";if(endsAt==null)return "Iniciada · sin hora final";return now.isBefore(endsAt)?"Ahora":"Terminada";}
  public String startLocal(){return local(startsAt,zoneId);} public String endLocal(){return endsAt==null?"":local(endsAt,zoneId);}
  public String schedule(){return DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(startsAt.atZone(ZoneId.of(zoneId)))+(endsAt==null?"":" – "+DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(endsAt.atZone(ZoneId.of(zoneId))))+" · "+zoneId;}
 }
 private static String local(Instant time,String zone){return DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm").format(time.atZone(ZoneId.of(zone)));}
 private final org.springframework.jdbc.core.RowMapper<Agenda> agendaMapper=(r,n)->new Agenda(r.getLong("id"),r.getLong("event_id"),r.getString("title"),r.getString("description"),Instant.ofEpochMilli(r.getLong("starts_at")),r.getObject("ends_at")==null?null:Instant.ofEpochMilli(r.getLong("ends_at")),r.getString("zone_id"),r.getString("place"),r.getObject("point_id",Long.class),r.getBoolean("active"),r.getLong("version"),r.getString("request_key"));
 public List<Agenda> agenda(long event){events.get(event);return sql.query("select * from event_agenda_items where event_id=? and active=true order by starts_at,id",agendaMapper,event);}
 public List<Agenda> managedAgenda(long event){manage(event);return sql.query("select * from event_agenda_items where event_id=? order by starts_at,id",agendaMapper,event);}
 private Agenda item(long event,long id){return sql.query("select * from event_agenda_items where event_id=? and id=?",agendaMapper,event,id).stream().findFirst().orElseThrow(()->new NotFoundException("No encontramos esa actividad en este evento."));}
 public AgendaForm agendaForm(long event,Long id){manage(event);var f=new AgendaForm();f.setZoneId(settings(event).zoneId());if(id==null)return f;var a=item(event,id);f.setVersion(a.version());f.setRequestKey(a.requestKey());f.setTitle(a.title());f.setDescription(a.description());f.setStartsAt(a.startLocal());f.setEndsAt(a.endLocal());f.setZoneId(a.zoneId());f.setPlace(a.place());f.setPointId(a.pointId());f.setActive(a.active());return f;}
 private static Instant time(String value,ZoneId zone){try{var local=LocalDateTime.parse(value);var offsets=zone.getRules().getValidOffsets(local);if(offsets.size()!=1)throw new IllegalArgumentException();return local.toInstant(offsets.getFirst());}catch(RuntimeException ex){throw new BusinessRuleException("Revisa la fecha y hora. Debe existir y ser inequívoca en la zona elegida.");}}
 @Transactional(isolation=Isolation.READ_COMMITTED)
 public long saveAgenda(long event,Long id,@Valid AgendaForm f){manage(event);events.lock(event);
  try{if(!UUID.fromString(f.getRequestKey()).toString().equalsIgnoreCase(f.getRequestKey()))throw new IllegalArgumentException();}catch(IllegalArgumentException ex){throw new BusinessRuleException("Vuelve a abrir el formulario de agenda.");}
  if(id==null){var prior=sql.query("select id from event_agenda_items where event_id=? and request_key=?",(r,n)->r.getLong(1),event,f.getRequestKey());if(!prior.isEmpty())return prior.getFirst();}
  else revision(f.getVersion(),item(event,id).version());
  var zone=zone(f.getZoneId());var start=time(f.getStartsAt(),zone);var end=f.getEndsAt().isBlank()?null:time(f.getEndsAt(),zone);
  if(end!=null&&!end.isAfter(start))throw new BusinessRuleException("La hora final debe ser posterior al inicio.");
  if(f.getPointId()!=null&&encounters.points(event).stream().noneMatch(p->p.getId().equals(f.getPointId())&&!p.isArchived()))throw new BusinessRuleException("Elige un punto existente de este evento.");
  if(id==null){sql.update("insert into event_agenda_items(event_id,request_key,title,description,starts_at,ends_at,zone_id,place,point_id,active,version) values(?,?,?,?,?,?,?,?,?,?,0)",event,f.getRequestKey(),f.getTitle().strip(),f.getDescription().strip(),start.toEpochMilli(),end==null?null:end.toEpochMilli(),zone.getId(),f.getPlace().strip(),f.getPointId(),f.isActive());return sql.queryForObject("select id from event_agenda_items where event_id=? and request_key=?",Long.class,event,f.getRequestKey());}
  sql.update("update event_agenda_items set title=?,description=?,starts_at=?,ends_at=?,zone_id=?,place=?,point_id=?,active=?,version=version+1 where event_id=? and id=?",f.getTitle().strip(),f.getDescription().strip(),start.toEpochMilli(),end==null?null:end.toEpochMilli(),zone.getId(),f.getPlace().strip(),f.getPointId(),f.isActive(),event,id);return id;
 }
 public record Personal(String eventName,long completed,long balance,Avatar avatar,boolean challengeEnabled,String alias,List<String> interests){public long progress(){return Math.min(2,completed);}public boolean reached(){return completed>=2;}}
 public Personal personal(long self){var p=participants.get(self);if(!auth.legacyFixture()&&!auth.owned(self)&&!auth.demo())throw new org.springframework.security.access.AccessDeniedException("Solo tu experiencia.");long count=sql.queryForObject("select count(*) from ping_matches where event_id=? and status='COMPLETED' and (first_id=? or second_id=?)",Long.class,p.eventId(),self,self);return new Personal(p.eventName(),count,p.points(),p.avatar(),settings(p.eventId()).challengeEnabled(),p.alias(),p.interests().stream().limit(3).toList());}
 public record Preparation(String title,String kind,String state,String edit){}
 public List<Preparation> preparation(long id){manage(id);var e=events.get(id);String base="/events/"+id;var points=encounters.points(id).stream().filter(p->p.isActive()&&!p.isArchived()).toList();var tasks=encounters.activities(id).stream().filter(a->a.isActive()&&!a.isArchived()).toList();var benefits=rewards.catalog(id);long compatible=tasks.stream().filter(a->points.stream().anyMatch(p->a.getPointIds().contains(p.getId()))).count();var result=new ArrayList<Preparation>();
  result.add(new Preparation("Estado del evento","Imprescindible para nuevas conexiones",e.getStatus().getLabel()+". Solo Abierto permite nuevas conexiones; los activos conservan sus reglas.",base));
  result.add(new Preparation("Código y enlace / QR","Entrada existente",e.getEntryCode()+" · código estable. "+(links.link(e)==null?"Dirección pública sin configurar; el código manual sigue disponible.":"Enlace y QR disponibles en el detalle."),base+"#entry-share-title"));
  result.add(new Preparation("Puntos de encuentro","Reglas actuales",e.isStructuredEncounter()?points.size()+" puntos activos. "+(points.isEmpty()?"La activación necesita un punto y actividad compatibles.":String.join(" · ",points.stream().map(MeetingPoint::getName).toList())):"Catálogo anterior: "+(EncounterOptions.unique(e.getMeetingPoints()).isEmpty()?e.getMeetingPoint():String.join(" · ",EncounterOptions.unique(e.getMeetingPoints())))+". Se conserva la compatibilidad.",base+"/encounters#points"));
  result.add(new Preparation("Actividades y puntos","Reglas actuales",e.isStructuredEncounter()?tasks.size()+" activas, "+compatible+" con punto activo compatible. "+String.join(" · ",tasks.stream().map(a->a.getTitle()+": "+a.getRewardPoints()+" puntos").toList()):"Catálogo anterior / por interés / neutral, 10 puntos. "+EncounterOptions.unique(e.getEncounterActivities()).size()+" actividades personalizadas. "+(EncounterOptions.unique(e.getMeetingPoints()).size()>3||EncounterOptions.unique(e.getEncounterActivities()).size()>10||EncounterOptions.unique(e.getEncounterWords()).size()>100?"Revisa los excesos anteriores: las reglas actuales requieren reducirlos antes de activar nuevos encuentros.":"Las opciones anteriores se conservan."),base+"/encounters#activities"));
  result.add(new Preparation("Plano y colocación","Opcional",e.getFloorplanId()==null?"Sin plano. Se mantienen puntos e indicaciones.":"Plano configurado · "+plans.points(id).stream().filter(FloorplanService.Point::active).filter(FloorplanService.Point::placed).count()+" puntos colocados; revisa los activos antes de la demo.",base+"/floorplan"));
  result.add(new Preparation("Beneficios y entrega","Opcional",benefits.isEmpty()?"Sin beneficios activos. No bloquea conexiones.":String.join(" · ",benefits.stream().map(b->b.getName()+" ("+b.getPointsCost()+" puntos): "+(b.isDefaultDelivery()?(e.getDeliveryName().isBlank()?"ubicación de entrega por configurar":e.getDeliveryName()):(b.getClaimLocation().isBlank()?"consulta la entrega con organización":b.getClaimLocation()))).toList()),base+"/rewards"));
  result.add(new Preparation("Ayuda y contacto oficial","Recomendación para la demostración",settings(id).staffLocation().isBlank()?"Publica dónde encontrar al personal si el evento lo ofrece. No es obligatorio.":settings(id).staffLocation(),base+"/experience-settings"));
  result.add(new Preparation("Agenda general y reto","Opcionales",agenda(id).size()+" actividades publicadas · reto de dos conexiones "+(settings(id).challengeEnabled()?"activado":"desactivado")+". No otorgan puntos adicionales.",base+"/agenda"));return result;
 }
}
