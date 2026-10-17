package com.ping.match;

import com.ping.event.EventService;
import com.ping.participant.CompletionRewardService;
import com.ping.web.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** Cada método público confirma su propia transacción; ninguna abarca toda la Saga. */
@Service @Profile("!bootstrap")
@Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
public class MatchCompletionSagaSteps {
    private final MatchRepository matches;
    private final MatchCompletionRepository sagas;
    private final CompletionStepRepository history;
    private final MatchReservationRepository reservations;
    private final CompletionRewardService rewards;
    private final EventService events;
    public MatchCompletionSagaSteps(MatchRepository matches, MatchCompletionRepository sagas, CompletionStepRepository history,
            MatchReservationRepository reservations, CompletionRewardService rewards, EventService events) {
        this.matches = matches; this.sagas = sagas; this.history = history; this.reservations = reservations; this.rewards = rewards; this.events = events;
    }
    private Match lock(long id) {
        events.lock(matches.eventId(id).orElseThrow(() -> new NotFoundException("No encontramos esa conexión.")));
        return matches.findById(id).orElseThrow();
    }
    private void record(MatchCompletion saga, String action) { history.save(new CompletionStep(saga, action)); }
    public boolean begin(long id, long participantId) {
        Match match = lock(id);
        if (!match.owns(participantId)) throw new NotFoundException("Esta conexión no pertenece al participante de tu sesión demo.");
        if (match.getStatus() == MatchStatus.COMPLETED) return false; // Incluye matches anteriores a V8.
        if (match.getStatus() != MatchStatus.ACTIVE) throw new BusinessRuleException("Primero ambos deben aceptar la propuesta.");
        if (match.isCompletionPending()) return true;
        match.confirm(participantId);
        if (!match.bothConfirmed()) return false;
        MatchCompletion saga = sagas.findById(id).orElse(null);
        if (saga == null) saga = sagas.saveAndFlush(new MatchCompletion(id, participantId));
        else {
            if (saga.getStatus() != SagaStatus.COMPENSATED) throw new BusinessRuleException("La finalización requiere recuperación antes de reintentar.");
            saga.restart(participantId);
        }
        match.prepareCompletion(); record(saga, "PREPARE_MATCH"); return true;
    }
    public SagaStatus state(long id) { return sagas.findById(id).orElseThrow().getStatus(); }
    public void grantFirst(long id) {
        Match match = lock(id); MatchCompletion saga = sagas.findById(id).orElseThrow();
        if (saga.getStatus() != SagaStatus.MATCH_PREPARED) return;
        rewards.grant(match.getFirst().getId()); saga.firstGranted(); record(saga, "GRANT_FIRST");
    }
    public void grantSecond(long id) {
        Match match = lock(id); MatchCompletion saga = sagas.findById(id).orElseThrow();
        if (saga.getStatus() != SagaStatus.FIRST_REWARD_GRANTED) return;
        rewards.grant(match.getSecond().getId()); saga.secondGranted(); record(saga, "GRANT_SECOND");
    }
    public void close(long id) {
        Match match = lock(id); MatchCompletion saga = sagas.findById(id).orElseThrow();
        if (saga.getStatus() != SagaStatus.SECOND_REWARD_GRANTED) return;
        if (!saga.isFirstGranted() || !saga.isSecondGranted()) throw new IllegalStateException("Reward receipts missing");
        match.complete(); match.getFirst().restartWaiting(); match.getSecond().restartWaiting(); reservations.deleteByMatchId(id);
        saga.completed(); record(saga, "CLOSE");
    }
    public void startCompensation(long id) {
        lock(id); MatchCompletion saga = sagas.findById(id).orElseThrow();
        if (saga.getStatus() != SagaStatus.COMPLETED && saga.getStatus() != SagaStatus.COMPENSATED) saga.compensating();
    }
    public void reverseSecond(long id) {
        Match match = lock(id); MatchCompletion saga = sagas.findById(id).orElseThrow();
        if (!compensating(saga) || !saga.isSecondGranted()) return;
        rewards.reverse(match.getSecond().getId()); saga.reverseSecond(); record(saga, "REVERSE_SECOND");
    }
    public void reverseFirst(long id) {
        Match match = lock(id); MatchCompletion saga = sagas.findById(id).orElseThrow();
        if (!compensating(saga) || !saga.isFirstGranted()) return;
        rewards.reverse(match.getFirst().getId()); saga.reverseFirst(); record(saga, "REVERSE_FIRST");
    }
    public void restore(long id) {
        Match match = lock(id); MatchCompletion saga = sagas.findById(id).orElseThrow();
        if (!compensating(saga)) return;
        if (saga.isFirstGranted() || saga.isSecondGranted()) throw new IllegalStateException("Rewards still pending reversal");
        match.restoreCompletion(saga.getTriggerParticipantId()); saga.compensated(); record(saga, "RESTORE_MATCH");
    }
    public void failed(long id) {
        lock(id); MatchCompletion saga = sagas.findById(id).orElseThrow();
        if (saga.getStatus() != SagaStatus.COMPLETED && saga.getStatus() != SagaStatus.COMPENSATED) saga.failed();
    }
    private boolean compensating(MatchCompletion saga) { return saga.getStatus() == SagaStatus.COMPENSATING || saga.getStatus() == SagaStatus.FAILED; }
}
