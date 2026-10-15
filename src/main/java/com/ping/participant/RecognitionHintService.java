package com.ping.participant;

import com.ping.event.EventService;
import com.ping.match.*;
import com.ping.web.*;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.validation.annotation.Validated;

@Service @Validated @Profile("!bootstrap") @Transactional(readOnly = true)
public class RecognitionHintService {
    public record AuthorizedHint(String text, String revision) { }
    private final ParticipantRepository participants;
    private final MatchRepository matches;
    private final EventService events;
    public RecognitionHintService(ParticipantRepository participants, MatchRepository matches, EventService events) {
        this.participants = participants; this.matches = matches; this.events = events;
    }
    private Participant own(long self) { return participants.findById(self).orElseThrow(() -> new NotFoundException("No encontramos ese participante.")); }
    public RecognitionHintForm ownForm(long self) {
        var participant = own(self); var form = new RecognitionHintForm();
        form.setHint(participant.getRecognitionHint() == null ? "" : participant.getRecognitionHint()); form.setVersion(participant.getVersion()); return form;
    }
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void save(long self, @Valid RecognitionHintForm form) {
        events.lock(participants.eventId(self).orElseThrow(() -> new NotFoundException("No encontramos ese participante.")));
        var participant = own(self);
        if (form.getVersion() == null || participant.getVersion() != form.getVersion())
            throw new BusinessRuleException("Tu información cambió. Conservamos tu borrador; revisa y vuelve a guardar.");
        String hint = form.getHint() == null ? "" : form.getHint().strip();
        participant.changeRecognitionHint(hint.isBlank() ? null : hint);
        participants.flush();
    }
    public AuthorizedHint other(long matchId, long self) {
        var match = matches.findById(matchId).orElseThrow(() -> new NotFoundException("No encontramos esa conexión."));
        if (!match.owns(self)) throw new NotFoundException("Esta conexión no pertenece al participante de tu sesión demo.");
        if (match.getStatus() != MatchStatus.ACTIVE || !match.accepted(match.getFirst().getId()) || !match.accepted(match.getSecond().getId())) return null;
        var other = match.getFirst().getId() == self ? match.getSecond() : match.getFirst();
        return new AuthorizedHint(other.getRecognitionHint(), matchId + ":" + other.getVersion());
    }
}
