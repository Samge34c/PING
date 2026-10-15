package com.ping.moderation;
import com.ping.match.*;
import com.ping.event.EventService;
import com.ping.web.*;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
@Service @Profile("!bootstrap") @Transactional(readOnly = true)
public class ModerationService {
    public record ReportView(long matchId, String reporter, String other, String reason, String comment, java.time.Instant createdAt) { }
    public record Status(boolean blocked, boolean reported) { }
    private final BlockRepository blocks;
    private final ReportRepository reports;
    private final MatchRepository matches;
    private final EventService events;
    public ModerationService(BlockRepository blocks, ReportRepository reports, MatchRepository matches, EventService events) {
        this.blocks = blocks; this.reports = reports; this.matches = matches; this.events = events;
    }
    private Match owned(long matchId, long self, boolean lock) {
        if (lock) events.lock(matches.eventId(matchId).orElseThrow(() -> new NotFoundException("No encontramos esa conexión.")));
        Match match = matches.findById(matchId).orElseThrow(() -> new NotFoundException("No encontramos esa conexión."));
        if (!match.owns(self)) throw new NotFoundException("Solo puedes gestionar tus propias conexiones.");
        return match;
    }
    private long other(Match match, long self) { return match.getFirst().getId() == self ? match.getSecond().getId() : match.getFirst().getId(); }
    public Status status(long matchId, long self) {
        Match m = owned(matchId, self, false);
        return new Status(blocks.existsByBlockerIdAndBlockedId(self, other(m, self)), reports.findByMatchIdAndReporterId(matchId, self).isPresent());
    }
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void block(long matchId, long self) {
        Match m = owned(matchId, self, true); long other = other(m, self);
        if (!blocks.existsByBlockerIdAndBlockedId(self, other)) blocks.save(new ParticipantBlock(self, other));
    }
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void report(long matchId, long self, ReportReason reason, String comment) {
        Match m = owned(matchId, self, true);
        if (m.getStatus() != MatchStatus.COMPLETED && m.getStatus() != MatchStatus.CANCELLED)
            throw new BusinessRuleException("Puedes reportar después de terminar o cancelar la interacción.");
        if (reason == null) throw new BusinessRuleException("Selecciona un motivo de reporte.");
        String text = Objects.requireNonNullElse(comment, "").strip();
        if (text.length() > 300) throw new BusinessRuleException("El comentario puede tener hasta 300 caracteres.");
        if (reports.findByMatchIdAndReporterId(matchId, self).isEmpty()) reports.save(new InteractionReport(m, self, reason, text));
    }
    public List<ReportView> forEvent(long eventId) {
        return reports.forEvent(eventId).stream().map(r -> {
            var m = r.getMatch(); boolean first = m.getFirst().getId() == r.getReporterId();
            return new ReportView(m.getId(), first ? m.getFirst().getAlias() : m.getSecond().getAlias(), first ? m.getSecond().getAlias() : m.getFirst().getAlias(), r.getReason().getLabel(), r.getComment(), r.getCreatedAt());
        }).toList();
    }
}
