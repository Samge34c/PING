package com.ping.review;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import com.ping.auth.*;
import com.ping.event.EventService;
import com.ping.match.*;
import com.ping.web.*;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/**
 * DEC-066. Staff review of an encounter task. It records what the pair sent and the staff decision.
 * It never grants points: approval only lets the existing completion Saga start once both confirmed.
 */
@Service @Profile("!bootstrap") @Transactional(readOnly = true)
public class ActivityReviewService {
    public enum Status {
        PENDING("Pendiente de revisión"), APPROVED("Aprobada"), CHANGES("Necesita corrección");
        public final String label;
        Status(String label) { this.label = label; }
        public String getLabel() { return label; }
    }
    public record Review(long matchId, Status status, String response, String comment, Instant submittedAt, Instant reviewedAt, long revision) { }
    public record Item(long matchId, String firstAlias, String secondAlias, String activityTitle, String checkInstructions,
                       String responsePrompt, String response, Status status, String comment, Instant submittedAt,
                       boolean ownEncounter, int rewardPoints, int confirmations, String matchStatus) {
        public boolean decidable() { return !ownEncounter && "ACTIVE".equals(matchStatus) && status != Status.APPROVED; }
    }
    public static final int MAX_RESPONSE = 1000, MAX_COMMENT = 500;

    private final JdbcTemplate jdbc;
    private final EventService events;
    private final MatchRepository matches;
    private final AuthSupport auth;
    private final AccountService accounts;
    public ActivityReviewService(JdbcTemplate jdbc, EventService events, MatchRepository matches, AuthSupport auth, AccountService accounts) {
        this.jdbc = jdbc; this.events = events; this.matches = matches; this.auth = auth; this.accounts = accounts;
    }

    public Review get(long matchId) {
        return jdbc.query("select match_id,status,response,reviewer_comment,submitted_at,reviewed_at,revision from activity_reviews where match_id=?",
                (r, n) -> new Review(r.getLong(1), Status.valueOf(r.getString(2)), r.getString(3), r.getString(4),
                        r.getTimestamp(5).toInstant(), r.getTimestamp(6) == null ? null : r.getTimestamp(6).toInstant(), r.getLong(7)), matchId)
            .stream().findFirst().orElse(null);
    }

    /** Participant side: save or correct the pair's shared submission. Saving is not approving. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void submit(long matchId, long self, String response) {
        events.lock(matches.eventId(matchId).orElseThrow(() -> new NotFoundException("No encontramos esa conexión.")));
        Match m = matches.findById(matchId).orElseThrow(() -> new NotFoundException("No encontramos esa conexión."));
        if (!m.owns(self)) throw new NotFoundException("Esta conexión no pertenece al participante de tu sesión.");
        if (m.getStatus() != MatchStatus.ACTIVE || !m.staffReview()) throw new BusinessRuleException("Esta actividad no necesita revisión del personal.");
        if (m.getVerificationPolicy() >= 2 && (!m.bothArrived() || !m.bothVerified()))
            throw new BusinessRuleException("Primero registren ambas llegadas y verifiquen sus códigos personales.");
        String text = response == null ? "" : response.strip();
        if (text.length() > MAX_RESPONSE) throw new BusinessRuleException("La respuesta admite hasta " + MAX_RESPONSE + " caracteres.");
        if (!m.getResponsePrompt().isBlank() && text.isEmpty()) throw new BusinessRuleException("Escribe la respuesta que pide la actividad antes de enviarla.");
        var current = get(matchId);
        Timestamp now = Timestamp.from(Instant.now());
        if (current == null) {
            jdbc.update("insert into activity_reviews(match_id,event_id,status,response,submitted_by,submitted_at,revision) values(?,?,?,?,?,?,0)",
                    matchId, m.getEvent().getId(), Status.PENDING.name(), text, self, now);
            return;
        }
        if (current.status() == Status.APPROVED) throw new BusinessRuleException("El personal ya aprobó esta actividad. No hace falta enviarla otra vez.");
        if (current.status() == Status.PENDING && current.response().equals(text)) return; // Repeated submit: nothing changes.
        jdbc.update("update activity_reviews set status=?,response=?,submitted_by=?,submitted_at=?,revision=revision+1 where match_id=?",
                Status.PENDING.name(), text, self, now, matchId);
    }

    public long open(long event) {
        return jdbc.queryForObject("select count(*) from activity_reviews r join ping_matches m on m.id=r.match_id where r.event_id=? and r.status='PENDING' and m.status='ACTIVE'", Long.class, event);
    }

    /** Staff list for one event. Requires permission for that event; rows from other events are never read. */
    public List<Item> forEvent(long event) {
        auth.manage(event);
        long actor = auth.require().id();
        var owned = new HashSet<>(jdbc.queryForList("select participant_id from account_participations where account_id=?", Long.class, actor));
        return jdbc.query("""
                select r.match_id,f.alias,s.alias,m.activity_title,m.activity,m.check_instructions,m.response_prompt,r.response,r.status,
                       r.reviewer_comment,r.submitted_at,m.first_id,m.second_id,m.reward_points,m.first_confirmed,m.second_confirmed,m.status
                from activity_reviews r join ping_matches m on m.id=r.match_id
                join participants f on f.id=m.first_id join participants s on s.id=m.second_id
                where r.event_id=? and m.event_id=?
                order by case r.status when 'PENDING' then 0 when 'CHANGES' then 1 else 2 end, r.submitted_at desc
                """, (r, n) -> {
                    String title = r.getString(4) == null || r.getString(4).isBlank() ? r.getString(5) : r.getString(4);
                    return new Item(r.getLong(1), r.getString(2), r.getString(3), title, r.getString(6), r.getString(7), r.getString(8),
                            Status.valueOf(r.getString(9)), r.getString(10), r.getTimestamp(11).toInstant(),
                            owned.contains(r.getLong(12)) || owned.contains(r.getLong(13)), r.getInt(14),
                            (r.getBoolean(15) ? 1 : 0) + (r.getBoolean(16) ? 1 : 0), r.getString(17));
                }, event, event);
    }

    /**
     * Staff decision. Returns true when the task is approved (also on a repeated approval, so the caller can
     * retry the idempotent completion). Never touches balances.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public boolean decide(long event, long matchId, boolean approve, String comment) {
        auth.manage(event);
        long actor = auth.require().id();
        events.lock(event);
        Match m = matches.findById(matchId).orElseThrow(() -> new NotFoundException("No encontramos ese encuentro."));
        if (m.getEvent().getId() != event) throw new NotFoundException("Ese encuentro pertenece a otro evento.");
        if (accounts.owns(actor, m.getFirst().getId()) || accounts.owns(actor, m.getSecond().getId()))
            throw new AccessDeniedException("Otra persona autorizada debe revisar un encuentro en el que participas.");
        var review = get(matchId);
        if (review == null) throw new BusinessRuleException("Todavía no hay nada enviado para revisar en este encuentro.");
        if (review.status() == Status.APPROVED) {
            if (approve) return true;
            throw new BusinessRuleException("Esta actividad ya está aprobada; no se puede pedir corrección.");
        }
        if (m.getStatus() != MatchStatus.ACTIVE) throw new BusinessRuleException("El encuentro ya no está activo; no se puede revisar.");
        String note = comment == null ? "" : comment.strip();
        if (note.length() > MAX_COMMENT) throw new BusinessRuleException("El comentario admite hasta " + MAX_COMMENT + " caracteres.");
        if (!approve && note.isEmpty()) throw new BusinessRuleException("Explica qué deben corregir para que puedan reenviarla.");
        if (!approve && review.status() == Status.CHANGES && note.equals(review.comment())) return false;
        jdbc.update("update activity_reviews set status=?,reviewer_account_id=?,reviewer_comment=?,reviewed_at=?,revision=revision+1 where match_id=?",
                (approve ? Status.APPROVED : Status.CHANGES).name(), actor, note, Timestamp.from(Instant.now()), matchId);
        accounts.audit(actor, approve ? "APPROVE_TASK" : "REQUEST_TASK_CHANGES", matchId, event);
        return approve;
    }
}
