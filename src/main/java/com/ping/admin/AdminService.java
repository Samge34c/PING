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
    private final CompletionStepRepository credits;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    public AdminService(EventService events, ParticipantRepository participants, MatchRepository matches, FeedbackRepository feedback, CompletionStepRepository credits, org.springframework.jdbc.core.JdbcTemplate jdbc) {
        this.events = events; this.participants = participants; this.matches = matches; this.feedback = feedback; this.credits = credits; this.jdbc = jdbc;
    }
    /** DEC-079: one grouped query for every event; both members of each COMPLETED encounter, deduplicated by participant id (never by alias). */
    private Map<Long, Long> connectedPeople() {
        var out = new HashMap<Long, Long>();
        jdbc.query("""
                select x.event_id, count(distinct x.pid) from (
                  select event_id, first_id pid from ping_matches where status='COMPLETED'
                  union all
                  select event_id, second_id pid from ping_matches where status='COMPLETED') x
                group by x.event_id""", (org.springframework.jdbc.core.RowCallbackHandler) r -> out.put(r.getLong(1), r.getLong(2)));
        return out;
    }
    /** Receipts per event and state, one grouped query. */
    private Map<Long, EventMetrics.Claims> claimTotals() {
        var raw = new HashMap<Long, Map<String, Long>>();
        jdbc.query("select p.event_id, c.status, count(*) from reward_claims c join participants p on p.id=c.participant_id group by p.event_id, c.status",
                (org.springframework.jdbc.core.RowCallbackHandler) r -> raw.computeIfAbsent(r.getLong(1), k -> new HashMap<>()).put(r.getString(2), r.getLong(3)));
        var out = new HashMap<Long, EventMetrics.Claims>();
        raw.forEach((event, by) -> out.put(event, new EventMetrics.Claims(by.getOrDefault("REQUESTED", 0L), by.getOrDefault("DELIVERED", 0L), by.getOrDefault("INCIDENT", 0L), by.getOrDefault("CANCELLED", 0L))));
        return out;
    }
    private Map<Long, Integer> doorTotals() {
        var out = new HashMap<Long, Integer>();
        jdbc.query("select id, door_attendance from events where door_attendance is not null", (org.springframework.jdbc.core.RowCallbackHandler) r -> out.put(r.getLong(1), r.getInt(2)));
        return out;
    }
    /** DEC-079: optional door total; null clears it. Caller has checked that the account organises this event. */
    @Transactional
    public void recordDoor(long eventId, Integer total) {
        events.get(eventId);
        if (total != null && (total < 0 || total > 1_000_000)) throw new com.ping.web.BusinessRuleException("Escribe un número entre 0 y 1.000.000, o deja el campo vacío.");
        jdbc.update("update events set door_attendance=? where id=?", total, eventId);
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
                m.getStatus() == MatchStatus.COMPLETED ? (m.getRewardPoints() == 0 ? "Completada sin puntos" : "+"+m.getRewardPoints()+" puntos a cada persona") : m.getStatus() == MatchStatus.CANCELLED ? "Sin recompensa" : "En curso")).toList(),
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
        // Credit ledger, not balances: balances drop when points are spent on claims.
        var credited = credits.creditedByEvent().stream().collect(Collectors.toMap(CompletionStepRepository.CreditTotal::getEventId, CompletionStepRepository.CreditTotal::getTotal));
        var connected = connectedPeople(); var door = doorTotals(); var claims = claimTotals();
        return events.list().stream().map(e -> {
            var p = people.get(e.getId()); var m = counts.getOrDefault(e.getId(), Map.of()); var f = ratings.getOrDefault(e.getId(), Map.of());
            long completed = m.getOrDefault(MatchStatus.COMPLETED, 0L);
            return new EventMetrics(e.getId(), e.getName(), e.getStatus(), p == null ? 0 : p.getTotal(), p == null ? 0 : p.getAvailable(),
                e.canStartNewMatches() ? free.getOrDefault(e.getId(), 0L) : 0L, m.getOrDefault(MatchStatus.OFFERED, 0L), m.getOrDefault(MatchStatus.ACTIVE, 0L), completed,
                m.getOrDefault(MatchStatus.CANCELLED, 0L), credited.getOrDefault(e.getId(), 0L), f.getOrDefault(Feedback.Rating.USEFUL, 0L),
                f.getOrDefault(Feedback.Rating.NEUTRAL, 0L), f.getOrDefault(Feedback.Rating.NOT_USEFUL, 0L), connected.getOrDefault(e.getId(), 0L), door.get(e.getId()), claims.getOrDefault(e.getId(), EventMetrics.Claims.NONE));
        }).toList();
    }
}
