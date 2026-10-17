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
    public EventDetails details(long eventId) {
        var event = events.get(eventId);
        var connections = matches.findByEventIdOrderByIdDesc(eventId);
        Map<Long, Long> completed = new HashMap<>();
        connections.stream().filter(m -> m.getStatus() == MatchStatus.COMPLETED).forEach(m -> {
            completed.merge(m.getFirst().getId(), 1L, Long::sum); completed.merge(m.getSecond().getId(), 1L, Long::sum);
        });
        var people = participants.findByEventIdOrderByIdAsc(eventId);
        var aliases = people.stream().collect(Collectors.toMap(p -> p.getId(), p -> p.getAlias()));
        return new EventDetails(event.getMeetingPoint(), people.stream().map(p -> new EventDetails.Person(p.getId(), p.getAlias(),
            p.getInterests().stream().map(i -> i.getName()).sorted().toList(), p.isAvailable(), p.getPoints(), completed.getOrDefault(p.getId(), 0L))).toList(),
            connections.stream().map(m -> new EventDetails.Connection(m.getId(), m.getFirst().getAlias(), m.getSecond().getAlias(),
                m.getCommonInterests().isBlank() ? "Alternativo" : "Normal", m.getStatus().getLabel(), m.getWord(), m.getActivity(), m.getCreatedAt(),
                m.getStatus() == MatchStatus.COMPLETED ? "+"+m.getRewardPoints()+" puntos a cada persona" : m.getStatus() == MatchStatus.CANCELLED ? "Sin recompensa" : "En curso")).toList(),
            feedback.forEvent(eventId).stream().map(f -> new EventDetails.Opinion(f.matchId(), aliases.get(f.getParticipantId()),
                switch(f.getRating()) { case USEFUL -> "Positivo"; case NEUTRAL -> "Neutral"; case NOT_USEFUL -> "Negativo"; }, f.getCreatedAt())).toList());
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
