package com.ping.event;

import java.util.List;
import java.util.Objects;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import com.ping.web.BusinessRuleException;
import com.ping.web.NotFoundException;

@Service @Validated @Profile("!bootstrap") @Transactional(readOnly = true)
public class EventService {
    private final EventRepository repository;
    public EventService(EventRepository repository) { this.repository = repository; }
    public List<Event> list() { return repository.findAll(Sort.by(Sort.Direction.DESC, "createdAt", "id")); }
    public Event get(long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("No encontramos ese evento."));
    }
    public Event entryEvent(String code) {
        String normalized = code == null ? "" : code.strip().replaceAll("[\\s-]", "").toUpperCase(java.util.Locale.ROOT);
        if (!normalized.matches("[A-Z0-9]{5,32}")) throw new BusinessRuleException("Revisa el código del evento. Puedes copiarlo o escribirlo sin espacios.");
        Event event = repository.findByEntryCode(normalized).orElseThrow(() -> new NotFoundException("No encontramos ese código. Revísalo o pídeselo al organizador."));
        if (event.getStatus() != EventStatus.OPEN) throw new BusinessRuleException(switch (event.getStatus()) {
            case PAUSED -> "La entrada a este evento está en pausa. Conservamos tu borrador; inténtalo cuando se reanude.";
            case CLOSED -> "Este evento está cerrado y no admite nuevas entradas. Puedes usar el código de otro evento.";
            default -> "Este evento todavía no ha abierto la entrada. Consulta con el organizador.";
        });
        return event;
    }
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public Event lock(long id) { return repository.lockById(id).orElseThrow(() -> new NotFoundException("No encontramos ese evento.")); }
    private String point(EventForm form) { String value = Objects.requireNonNullElse(form.getMeetingPoint(), "").strip(); return value.isBlank() ? "Punto Ping del evento" : value; }
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public Event create(@Valid EventForm form) {
        EncounterOptions.newLimits(form);
        Event event = new Event(normalizedName(form), Objects.requireNonNullElse(form.getDescription(), "").strip());
        event.delivery(form); event.changeEncounterOptions(form); event.changeMeetingPoint(point(form)); return repository.save(event);
    }
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public Event update(long id, @Valid EventForm form) {
        Event event = lock(id);
        if (form.getVersion() == null || form.getVersion() != event.getVersion()) {
            throw new BusinessRuleException("El evento cambió mientras lo editabas. Vuelve a abrir el formulario.");
        }
        EncounterOptions.checkGrowth(form.getMeetingPoints(),event.getMeetingPoints(),3,"Puntos");
        EncounterOptions.checkGrowth(form.getEncounterActivities(),event.getEncounterActivities(),10,"Actividades");
        EncounterOptions.checkGrowth(form.getEncounterWords(),event.getEncounterWords(),100,"Palabras");
        event.delivery(form); event.changeEncounterOptions(form); event.changeMeetingPoint(point(form));
        event.changeDetails(normalizedName(form), Objects.requireNonNullElse(form.getDescription(), "").strip());
        return event;
    }
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public Event transition(long id, EventStatus target) {
        Event event = lock(id);
        boolean allowed = switch (event.getStatus()) {
            case DRAFT -> target == EventStatus.OPEN;
            case OPEN -> target == EventStatus.PAUSED || target == EventStatus.CLOSED;
            case PAUSED -> target == EventStatus.OPEN || target == EventStatus.CLOSED;
            case CLOSED -> false;
        };
        if (!allowed) throw new BusinessRuleException("No se puede realizar esa acción desde el estado actual del evento.");
        event.changeStatus(target);
        return event;
    }
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void section(long id,boolean delivery,@Valid EventSectionForm form){var e=lock(id);if(form.getVersion()==null||form.getVersion()!=e.getVersion())throw new BusinessRuleException("El evento cambió. Conserva tu borrador y comprueba la versión guardada antes de repetir.");if(delivery){var f=new EventForm();f.setDeliveryName(form.getDeliveryName());f.setDeliveryDirections(form.getDeliveryDirections());e.delivery(f);}else{if(form.getName().isBlank())throw new BusinessRuleException("Escribe el nombre del evento.");e.changeDetails(form.getName().strip(),form.getDescription().strip());}}
    private String normalizedName(EventForm form) {
        String name = form.getName().strip();
        if (name.isBlank()) throw new BusinessRuleException("Escribe el nombre del evento.");
        return name;
    }
}
