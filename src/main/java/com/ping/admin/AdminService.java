package com.ping.admin;
import java.util.*;
import java.util.stream.Collectors;
import com.ping.event.EventService;
import com.ping.participant.ParticipantRepository;
import com.ping.match.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @Profile("!bootstrap") @Transactional(readOnly = true)
public class AdminService {
    private final EventService events;
    private final ParticipantRepository participants;
    private final MatchRepository matches;
    private final FeedbackRepository feedback;
    public AdminService(EventService events, ParticipantRepository participants, MatchRepository matches, FeedbackRepository feedback) {
        this.events = events; this.participants = participants; this.matches = matches; this.feedback = feedback;
    }
    public List<EventMetrics> metrics() {
        var people = participants.eventTotals().stream().collect(Collectors.toMap(ParticipantRepository.Totals::getEventId, t -> t));
        var free = participants.freeTotals().stream().collect(Collectors.toMap(ParticipantRepository.FreeTotal::getEventId, ParticipantRepository.FreeTotal::getTotal));
        Map<Long, Map<MatchStatus, Long>> counts = new HashMap<>();
        matches.eventTotals().forEach(t -> counts.computeIfAbsent(t.getEventId(), k -> new EnumMap<>(MatchStatus.class)).put(t.getStatus(), t.getTotal()));
        Map<Long, Map<Feedback.Rating, Long>> ratings = new HashMap<>();
        feedback.eventTotals().forEach(t -> ratings.computeIfAbsent(t.getEventId(), k -> new EnumMap<>(Feedback.Rating.class)).put(t.getRating(), t.getTotal()));
        return events.list().stream().map(e -> {
            var p = people.get(e.getId()); var m = counts.getOrDefault(e.getId(), Map.of()); var f = ratings.getOrDefault(e.getId(), Map.of());
            long completed = m.getOrDefault(MatchStatus.COMPLETED, 0L);
            return new EventMetrics(e.getId(), e.getName(), e.getStatus(), p == null ? 0 : p.getTotal(), p == null ? 0 : p.getAvailable(),
                e.canStartNewMatches() ? free.getOrDefault(e.getId(), 0L) : 0L, m.getOrDefault(MatchStatus.OFFERED, 0L), m.getOrDefault(MatchStatus.ACTIVE, 0L), completed,
                m.getOrDefault(MatchStatus.CANCELLED, 0L), Math.multiplyExact(completed, 20), f.getOrDefault(Feedback.Rating.USEFUL, 0L),
                f.getOrDefault(Feedback.Rating.NEUTRAL, 0L), f.getOrDefault(Feedback.Rating.NOT_USEFUL, 0L));
        }).toList();
    }
}
