package com.ping.match;

import java.util.List;
import com.ping.web.BusinessRuleException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;

/** Coordinador local: un proceso de aplicación, commits independientes y recuperación durable. */
@Service @Profile("!bootstrap") @Transactional(propagation = Propagation.NOT_SUPPORTED)
public class MatchCompletionSaga {
    private final MatchCompletionSagaSteps steps;
    private final MatchCompletionRepository sagas;
    public MatchCompletionSaga(MatchCompletionSagaSteps steps, MatchCompletionRepository sagas) { this.steps = steps; this.sagas = sagas; }
    public synchronized void confirm(long matchId, long participantId) {
        if (steps.begin(matchId, participantId)) resume(matchId);
    }
    public synchronized void resume(long id) {
        SagaStatus status = steps.state(id);
        if (status == SagaStatus.COMPLETED || status == SagaStatus.COMPENSATED) return;
        if (status == SagaStatus.COMPENSATING || status == SagaStatus.FAILED) {
            compensate(id); throw new BusinessRuleException("La finalización anterior se compensó. Revisa las confirmaciones del encuentro antes de reintentar.");
        }
        try {
            steps.grantFirst(id); steps.grantSecond(id); steps.close(id);
        } catch (RuntimeException failure) {
            // El paso fallido ya hizo rollback; los pasos anteriores sí están confirmados.
            steps.startCompensation(id);
            if (steps.state(id) == SagaStatus.COMPLETED) return; // El cierre pudo confirmarse antes de perder su respuesta.
            compensate(id);
            throw new BusinessRuleException("No se pudo completar la conexión. Se compensaron los puntos de este intento; vuelve a confirmar.");
        }
    }
    private void compensate(long id) {
        try { steps.reverseSecond(id); steps.reverseFirst(id); steps.restore(id); }
        catch (RuntimeException failure) {
            steps.failed(id);
            throw new BusinessRuleException("La conexión sigue reservada mientras se recupera la finalización. No vuelvas a buscar; reintenta confirmar más tarde.");
        }
    }
    @EventListener(ApplicationReadyEvent.class)
    public synchronized void recoverPending() {
        for (var saga : sagas.findByStatusNotIn(List.of(SagaStatus.COMPLETED, SagaStatus.COMPENSATED))) {
            try { resume(saga.getMatchId()); }
            catch (BusinessRuleException expected) { /* Estado durable: compensado o pendiente de otro reintento. */ }
        }
    }
}
