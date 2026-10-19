package com.ping.participant;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import com.ping.event.*;
import com.ping.web.*;

@Service @Validated @Profile("!bootstrap") @Transactional(readOnly = true)
public class ParticipantService {
    private final ParticipantRepository repository;
    private final EventService events;
    private final com.ping.match.MatchReservationRepository reservations;
    public ParticipantService(ParticipantRepository repository, EventService events, com.ping.match.MatchReservationRepository reservations) { this.repository = repository; this.events = events; this.reservations = reservations; }
    public List<Event> registrationEvents() { return events.list().stream().filter(event -> event.getStatus() == EventStatus.OPEN).toList(); }
    public List<ParticipantView> list() { return repository.findAllByOrderByCreatedAtDescIdDesc().stream().map(this::view).toList(); }
    public ParticipantView get(long id) { return view(require(id)); }
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public ParticipantView create(@Valid ParticipantForm form) {
        Event event = events.lock(form.getEventId());
        if (event.getStatus() != EventStatus.OPEN) throw new BusinessRuleException("Por ahora, el registro de demo necesita un evento abierto.");
        String alias = form.getAlias().strip();
        if (alias.isBlank()) throw new BusinessRuleException("Escribe un alias.");
        return view(repository.save(new Participant(alias, event, form.isAvailable())));
    }
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public ParticipantView setAvailability(long id, @Valid AvailabilityForm form) {
        events.lock(repository.eventId(id).orElseThrow(() -> new NotFoundException("No encontramos ese participante.")));
        Participant participant = require(id);
        if (reservations.existsById(id)) throw new BusinessRuleException("Primero cancela o termina tu conexión antes de cambiar disponibilidad.");
        if (form.getVersion() != participant.getVersion()) throw new BusinessRuleException("La disponibilidad cambió. Recarga la página.");
        participant.changeAvailability(form.getAvailable());
        repository.flush();
        return view(participant);
    }
    public Avatar avatar(long id){return require(id).getAvatar();}
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void avatar(long id,Avatar value){events.lock(repository.eventId(id).orElseThrow());require(id).avatar(value);}
    private Participant require(long id) { return repository.findById(id).orElseThrow(() -> new NotFoundException("No encontramos ese participante.")); }
    private ParticipantView view(Participant participant) {
        return new ParticipantView(participant.getId(), participant.getAlias(), participant.getEvent().getId(), participant.getEvent().getName(), participant.isAvailable(), participant.getVersion(), participant.getInterests().stream().map(com.ping.interest.Interest::getName).sorted().toList(), participant.getPoints());
    }
}
