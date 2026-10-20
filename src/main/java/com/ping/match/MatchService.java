package com.ping.match;

import java.util.List;
import com.ping.event.EventService;
import com.ping.web.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Profile("!bootstrap") @Transactional(readOnly = true)
public class MatchService {
    private final com.ping.encounter.EncounterService encounters;
    private final com.ping.floorplan.FloorplanService floorplans;
    private final JourneyPolicy policy;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    private final MatchRepository matches;
    private final MatchReservationRepository reservations;
    private final FeedbackRepository feedback;
    private final EventService events;
    private final MatchCompletionSaga completion;
    public MatchService(MatchRepository matches, MatchReservationRepository reservations, FeedbackRepository feedback, EventService events, MatchCompletionSaga completion, com.ping.encounter.EncounterService encounters, com.ping.floorplan.FloorplanService floorplans,JourneyPolicy policy,org.springframework.jdbc.core.JdbcTemplate jdbc) {this.policy=policy;this.jdbc=jdbc;
        this.floorplans=floorplans;
        this.encounters=encounters;
        this.matches = matches; this.reservations = reservations; this.feedback = feedback; this.events = events; this.completion = completion;
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
        if (match.bothAccepted()) {
            var selected=encounters.assign(match.getEvent(),match.getCommonInterests(),match.getId());
            match.activate(selected.encounter()); match.policy(policy.version()); match.describeEncounter(selected.directions(),selected.title(),selected.rewardPoints());
            match.freezeFloorplan(floorplans.snapshot(match.getEvent(),selected.pointId()));
        }
    }
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public void arrive(long id, long participantId) {
        Match match = locked(id, participantId);
        if (match.getStatus() != MatchStatus.ACTIVE || !match.bothAccepted()) throw new BusinessRuleException("Solo puedes registrar tu llegada en una conexión activa aceptada por ambos.");
        match.arrive(participantId);
    }
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public void cancel(long id, long participantId) {
        Match match = locked(id, participantId);
        if (match.isCompletionPending()) throw new BusinessRuleException("La finalización está en curso. Reintenta confirmar antes de cancelar.");
        if (match.getStatus() == MatchStatus.CANCELLED) return;
        if (match.getStatus() == MatchStatus.COMPLETED) throw new BusinessRuleException("Una conexión completada no se puede cancelar.");
        match.cancel(); jdbc.update("update purpose_credentials set revoked=true where purpose='ENCOUNTER' and context_id=? and consumed=false",id);reservations.deleteByMatchId(id);
    }
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public void confirm(long id, long participantId) { completion.confirm(id, participantId); }
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
            active ? match.getMeetingPoint() : null, active ? match.getWord() : null, active || match.getStatus() == MatchStatus.COMPLETED ? match.getActivity() : null,
            rating, match.isCompletionPending(), match.getCreatedAt(), match.isArrivalRequired(), match.arrived(self), match.arrived(other), active ? match.getMeetingDirections() : null, active || match.getStatus()==MatchStatus.COMPLETED ? match.getActivityTitle() : null, match.getRewardPoints(), active ? match.getFloorplanId() : null, active ? match.getFloorplanX() : null, active ? match.getFloorplanY() : null,match.getVerificationPolicy(),match.verified(self),match.verified(other),(first?match.getSecond():match.getFirst()).getAvatar());
    }
}
