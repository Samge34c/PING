package com.ping.match;

import java.util.List;
import com.ping.event.EventService;
import com.ping.web.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Profile("!bootstrap") @Transactional(readOnly = true)
public class MatchService {
    private final MatchRepository matches;
    private final MatchReservationRepository reservations;
    private final FeedbackRepository feedback;
    private final EventService events;
    public MatchService(MatchRepository matches, MatchReservationRepository reservations, FeedbackRepository feedback, EventService events) {
        this.matches = matches; this.reservations = reservations; this.feedback = feedback; this.events = events;
    }
    public long completedCount() { return matches.countByStatus(MatchStatus.COMPLETED); }
    public List<MatchView> list(long participantId) {
        var ratings = feedback.findByParticipantId(participantId).stream().collect(java.util.stream.Collectors.toMap(Feedback::matchId, f -> f.getRating().name()));
        return matches.forParticipant(participantId).stream().map(m -> view(m, participantId, ratings.get(m.getId()))).toList();
    }
    public MatchView get(long matchId, long participantId) { return view(owned(matchId, participantId), participantId); }
    private Match owned(long id, long participantId) {
        Match match = matches.findById(id).orElseThrow(() -> new NotFoundException("No encontramos esa conexión."));
        if (!match.owns(participantId)) throw new NotFoundException("Esta conexión no pertenece al participante de tu sesión demo.");
        return match;
    }
    private Match locked(long id, long participantId) {
        events.lock(matches.eventId(id).orElseThrow(() -> new NotFoundException("No encontramos esa conexión.")));
        return owned(id, participantId);
    }
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public void accept(long id, long participantId) {
        Match match = locked(id, participantId);
        if (match.getStatus() == MatchStatus.ACTIVE || match.getStatus() == MatchStatus.COMPLETED) return;
        if (match.getStatus() != MatchStatus.OFFERED) throw new BusinessRuleException("Esta propuesta ya se canceló.");
        if (!match.getEvent().canStartNewMatches()) throw new BusinessRuleException("El evento debe estar abierto para aceptar una nueva conexión. Puedes cancelar la propuesta.");
        match.accept(participantId);
        if (match.bothAccepted()) match.activate(ActivityCatalog.word(match.getCommonInterests(), match.getId()), ActivityCatalog.activity(match.getCommonInterests()));
    }
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public void cancel(long id, long participantId) {
        Match match = locked(id, participantId);
        if (match.getStatus() == MatchStatus.CANCELLED) return;
        if (match.getStatus() == MatchStatus.COMPLETED) throw new BusinessRuleException("Una conexión completada no se puede cancelar.");
        match.cancel(); reservations.deleteByMatchId(id);
    }
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public void confirm(long id, long participantId) {
        Match match = locked(id, participantId);
        if (match.getStatus() == MatchStatus.COMPLETED) return;
        if (match.getStatus() != MatchStatus.ACTIVE) throw new BusinessRuleException("Primero ambos deben aceptar la propuesta.");
        match.confirm(participantId);
        if (match.bothConfirmed()) {
            match.complete(); match.getFirst().rewardCompletedMatch(); match.getSecond().rewardCompletedMatch(); reservations.deleteByMatchId(id);
        }
    }
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public void giveFeedback(long id, long participantId, Feedback.Rating rating) {
        Match match = locked(id, participantId);
        if (match.getStatus() != MatchStatus.COMPLETED) throw new BusinessRuleException("El feedback está disponible después de completar la conexión.");
        if (rating == null) throw new BusinessRuleException("Selecciona una opción válida de feedback.");
        if (feedback.findByMatchIdAndParticipantId(id, participantId).isEmpty()) feedback.save(new Feedback(match, participantId, rating));
    }
    private MatchView view(Match match, long self) {
        return view(match, self, feedback.findByMatchIdAndParticipantId(match.getId(), self).map(f -> f.getRating().name()).orElse(null));
    }
    private MatchView view(Match match, long self, String rating) {
        boolean first = match.getFirst().getId() == self;
        long other = first ? match.getSecond().getId() : match.getFirst().getId();
        boolean active = match.getStatus() == MatchStatus.ACTIVE;
        return new MatchView(match.getId(), match.getStatus(), first ? match.getSecond().getAlias() : match.getFirst().getAlias(),
            match.getCommonInterests(), first ? match.getSecondInterests() : match.getFirstInterests(),
            match.accepted(self), match.accepted(other), match.confirmed(self), match.confirmed(other),
            active ? match.getMeetingPoint() : null, active ? match.getWord() : null, active ? match.getActivity() : null,
            rating);
    }
}
