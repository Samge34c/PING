package com.ping.interest;

import java.util.LinkedHashSet;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import com.ping.participant.*;
import com.ping.web.*;

@Service @Validated @Profile("!bootstrap") @Transactional(readOnly = true)
public class InterestService {
    private final InterestCatalogService catalog;
    private final InterestRepository repository;
    private final ParticipantRepository participants;
    private final com.ping.event.EventService events;
    public InterestService(InterestRepository repository, ParticipantRepository participants, com.ping.event.EventService events, InterestCatalogService catalog) { this.catalog=catalog; this.repository = repository; this.participants = participants; this.events = events; }
    public List<Interest> catalog() { return repository.findAllByOrderByNameAsc(); }
    public List<Interest> availableCatalog(){return catalog().stream().filter(Interest::isSelectable).toList();}
    public List<InterestCatalogService.Group> groups(java.util.Collection<Long> selected){return catalog.groups(selected);}
    public InterestSelectionForm selection(long participantId) {
        Participant participant = require(participantId);
        InterestSelectionForm form = new InterestSelectionForm();
        form.setInterestIds(participant.getInterests().stream().map(Interest::getId).toList());
        form.setVersion(participant.getVersion());
        return form;
    }
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public void save(long participantId, @Valid InterestSelectionForm form) {
        events.lock(participants.eventId(participantId).orElseThrow(() -> new NotFoundException("No encontramos ese participante.")));
        catalog.lock();
        Participant participant = require(participantId);
        if (form.getVersion() != participant.getVersion()) throw new BusinessRuleException("El participante cambió. Recarga sus intereses antes de guardar.");
        var ids = new LinkedHashSet<>(form.getInterestIds());
        List<Interest> selected = repository.findAllById(ids);
        if (selected.size() != ids.size()) throw new BusinessRuleException("Uno de los intereses no está en el catálogo. Recarga la página.");
        catalog.validateSelection(ids,participant.getInterests().stream().map(Interest::getId).toList());
        participant.replaceInterests(selected);
        participants.flush();
    }
    private Participant require(long id) { return participants.findById(id).orElseThrow(() -> new NotFoundException("No encontramos ese participante.")); }
}
