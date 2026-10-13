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
    private final InterestRepository repository;
    private final ParticipantRepository participants;
    public InterestService(InterestRepository repository, ParticipantRepository participants) { this.repository = repository; this.participants = participants; }
    public List<Interest> catalog() { return repository.findAllByOrderByNameAsc(); }
    public InterestSelectionForm selection(long participantId) {
        Participant participant = require(participantId);
        InterestSelectionForm form = new InterestSelectionForm();
        form.setInterestIds(participant.getInterests().stream().map(Interest::getId).toList());
        form.setVersion(participant.getVersion());
        return form;
    }
    @Transactional public void save(long participantId, @Valid InterestSelectionForm form) {
        Participant participant = require(participantId);
        if (form.getVersion() != participant.getVersion()) throw new BusinessRuleException("El participante cambió. Recarga sus intereses antes de guardar.");
        var ids = new LinkedHashSet<>(form.getInterestIds());
        List<Interest> selected = repository.findAllById(ids);
        if (selected.size() != ids.size()) throw new BusinessRuleException("Uno de los intereses no está en el catálogo. Recarga la página.");
        participant.replaceInterests(selected);
        participants.flush();
    }
    private Participant require(long id) { return participants.findById(id).orElseThrow(() -> new NotFoundException("No encontramos ese participante.")); }
}
