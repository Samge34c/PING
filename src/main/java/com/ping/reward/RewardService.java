package com.ping.reward;
import com.ping.event.EventService;
import com.ping.participant.ParticipantRepository;
import com.ping.match.MatchRepository;
import com.ping.web.*;
import java.util.*;
import java.time.Instant;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
@Service @Profile("!bootstrap") @Transactional(readOnly = true)
public class RewardService {
    public record ClaimView(long id, String alias, String benefit, long cost, ClaimStatus status, Instant createdAt,String description,String claimLocation,String claimInstructions) { }
    public record Summary(List<ClaimView> claims, long requested, long delivered, long incidents) { }
    private final com.ping.match.JourneyPolicy policy;
    private final com.ping.auth.AuthSupport auth;
    private final BenefitRepository benefits;
    private final RewardClaimRepository claims;
    private final ParticipantRepository participants;
    private final MatchRepository matches;
    private final EventService events;
    public RewardService(BenefitRepository benefits, RewardClaimRepository claims, ParticipantRepository participants, MatchRepository matches, EventService events,com.ping.match.JourneyPolicy policy,com.ping.auth.AuthSupport auth) {this.policy=policy;this.auth=auth;
        this.benefits = benefits; this.claims = claims; this.participants = participants; this.matches = matches; this.events = events;
    }
    public List<Benefit> catalog() { return benefits.findByActiveTrueAndEventIdIsNullOrderByIdAsc(); }
    public List<Benefit> catalog(long event) { return benefits.catalogForEvent(event).stream().sorted(Comparator.comparing((Benefit b)->b.getEventId()==null).thenComparing(Benefit::getId)).toList(); }
    public List<ClaimView> personal(long self) { return claims.findByParticipantIdOrderByIdDesc(self).stream().map(this::view).toList(); }
    public boolean pendingCompletion(long self) { return matches.hasPendingCompletion(self); }
    private ClaimView view(RewardClaim c) { return new ClaimView(c.getId(), c.getParticipant().getAlias(), c.getBenefitName(), c.getCost(), c.getStatus(), c.getCreatedAt(),c.getBenefitDescription(),c.getClaimLocation(),c.getClaimInstructions()); }
    public Summary forEvent(long eventId) {
        var rows = claims.forEvent(eventId).stream().map(this::view).toList();
        return new Summary(rows, rows.stream().filter(c -> c.status() == ClaimStatus.REQUESTED).count(), rows.stream().filter(c -> c.status() == ClaimStatus.DELIVERED).count(), rows.stream().filter(c -> c.status() == ClaimStatus.INCIDENT).count());
    }
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public long request(long self, long benefitId, String requestKey) {
        if (requestKey == null || !requestKey.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))
            throw new BusinessRuleException("Vuelve a abrir Beneficios para solicitar el canje.");
        requestKey = requestKey.toLowerCase(Locale.ROOT);
        events.lock(participants.eventId(self).orElseThrow(() -> new NotFoundException("No encontramos ese participante.")));
        var previous = claims.findByParticipantIdAndRequestKey(self, requestKey);
        if (previous.isPresent()) {
            if (previous.get().getBenefit().getId() != benefitId) throw new BusinessRuleException("Esa solicitud corresponde a otro beneficio. Recarga Beneficios.");
            return previous.get().getId();
        }
        if (pendingCompletion(self)) throw new BusinessRuleException("Espera a que termine la finalización de tu conexión antes de canjear puntos.");
        var benefit = benefits.findById(benefitId).orElseThrow(() -> new NotFoundException("No encontramos ese beneficio."));
        if (benefit.getEventId()!=null && !benefit.getEventId().equals(participants.eventId(self).orElseThrow())) throw new NotFoundException("Ese beneficio pertenece a otro evento.");
        if (!benefit.isActive()) throw new BusinessRuleException("Este beneficio ya no está disponible.");
        var participant = participants.findById(self).orElseThrow();
        participant.spendPoints(benefit.getPointsCost());
        var claim=new RewardClaim(participant,benefit,requestKey);claim.policy(policy.version());if(benefit.isDefaultDelivery())claim.defaultDelivery(participant.getEvent());return claims.save(claim).getId();
    }
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public long finish(long id, ClaimStatus target) {
        long eventId = claims.eventId(id).orElseThrow(() -> new NotFoundException("No encontramos ese canje."));
        events.lock(eventId);
        var claim = claims.findById(id).orElseThrow();
        if(!auth.legacyFixture()){auth.manage(eventId);if(auth.owned(claim.getParticipant().getId()))throw new org.springframework.security.access.AccessDeniedException("Otra persona autorizada debe registrar la entrega.");}
        if(!auth.legacyFixture()||claim.getReceiptPolicy()>=2)throw new BusinessRuleException("Valida el código del recibo antes de registrar la entrega o incidencia.");
        if (target == null || target == ClaimStatus.REQUESTED) throw new BusinessRuleException("Selecciona entrega o incidencia.");
        if (claim.getStatus() == target) return eventId;
        if (claim.getStatus() != ClaimStatus.REQUESTED) throw new BusinessRuleException("Este canje ya tiene un resultado guardado. No se modificaron puntos.");
        claim.finish(target); return eventId;
    }
}
