package com.ping.roster;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import com.ping.auth.*;
import com.ping.event.EventService;
import com.ping.match.MatchService;
import com.ping.participant.ParticipantRepository;
import com.ping.reward.RewardService;
import com.ping.web.*;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/**
 * DEC-068. Participants of ONE event: search by alias, withdraw and readmit. Only general administration or the
 * event's ORGANIZER role (checked here, not only in the page). Withdrawing keeps the account, history, points already
 * registered and deliveries made; it only affects this event's participation.
 */
@Service @Profile("!bootstrap") @Transactional(readOnly = true)
public class EventParticipantsService {
    public record Row(long id, String alias, long points, boolean withdrawn, boolean available, boolean inEncounter) { }
    public record HistoryItem(String action, String actor, Instant at, String reason) {
        public boolean withdrawal() { return "WITHDRAW".equals(action); }
    }
    public record Detail(long id, String alias, long eventId, String eventName, boolean withdrawn, long points,
                         int openEncounters, int pendingClaims, List<HistoryItem> history) { }
    public record Outcome(String alias, int encounters, int claims) { }
    static final int MAX_ROWS = 60;

    private final AuthSupport auth; private final AccountService accounts; private final EventService events;
    private final ParticipantRepository participants; private final MatchService matches; private final RewardService rewards; private final JdbcTemplate jdbc;
    public EventParticipantsService(AuthSupport auth, AccountService accounts, EventService events, ParticipantRepository participants,
                                    MatchService matches, RewardService rewards, JdbcTemplate jdbc) {
        this.auth = auth; this.accounts = accounts; this.events = events; this.participants = participants; this.matches = matches; this.rewards = rewards; this.jdbc = jdbc;
    }

    public List<Row> search(long event, String query) {
        auth.organize(event); events.get(event);
        String q = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        if (q.length() > 80) q = q.substring(0, 80);
        // '!' as LIKE escape: a backslash literal is parsed differently by MySQL and H2.
        String like = "%" + q.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        return jdbc.query("""
                select p.id, p.alias, p.points, p.withdrawn, p.available, (select count(*) from match_reservations r where r.participant_id=p.id) busy
                from participants p where p.event_id=? and lower(p.alias) like ? escape '!' order by p.withdrawn desc, lower(p.alias), p.id limit ?""",
                (r, n) -> new Row(r.getLong(1), r.getString(2), r.getLong(3), r.getBoolean(4), r.getBoolean(5), r.getLong(6) > 0), event, like, MAX_ROWS);
    }
    public long count(long event) { auth.organize(event); return jdbc.queryForObject("select count(*) from participants where event_id=?", Long.class, event); }

    public Detail detail(long event, long participant) {
        auth.organize(event); var e = events.get(event); var p = owned(event, participant);
        int open = jdbc.queryForObject("select count(*) from ping_matches where (first_id=? or second_id=?) and status in ('OFFERED','ACTIVE')", Integer.class, participant, participant);
        int claims = jdbc.queryForObject("select count(*) from reward_claims where participant_id=? and status='REQUESTED'", Integer.class, participant);
        var history = jdbc.query("select w.action, a.email, w.created_at, w.reason from participant_withdrawals w join ping_accounts a on a.id=w.actor_id where w.participant_id=? order by w.id desc",
                (r, n) -> new HistoryItem(r.getString(1), r.getString(2), r.getTimestamp(3).toInstant(), r.getString(4)), participant);
        return new Detail(p.getId(), p.getAlias(), event, e.getName(), p.isWithdrawn(), p.getPoints(), open, claims, history);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Outcome withdraw(long event, long participant, String reason) {
        auth.organize(event); long actor = auth.require().id();
        events.lock(event); var p = owned(event, participant);
        String why = reason == null ? "" : reason.strip();
        if (why.length() < 3) throw new BusinessRuleException("Escribe el motivo del retiro. Solo lo ve la organización.");
        if (why.length() > 300) throw new BusinessRuleException("El motivo admite hasta 300 caracteres.");
        if (accounts.owns(actor, participant)) throw new BusinessRuleException("No puedes retirarte a ti mismo. Pide a otra persona de la organización que lo haga.");
        if (p.isWithdrawn()) return new Outcome(p.getAlias(), 0, 0); // Repeating the action changes nothing.
        int encounters = matches.cancelForWithdrawal(participant);
        int claims = rewards.cancelForWithdrawal(participant);
        p.withdraw();
        history(participant, event, "WITHDRAW", actor, why);
        accounts.audit(actor, "WITHDRAW_PARTICIPANT", participant, event);
        return new Outcome(p.getAlias(), encounters, claims);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public String readmit(long event, long participant) {
        auth.organize(event); long actor = auth.require().id();
        events.lock(event); var p = owned(event, participant);
        if (!p.isWithdrawn()) return p.getAlias();
        p.readmit();
        history(participant, event, "READMIT", actor, "");
        accounts.audit(actor, "READMIT_PARTICIPANT", participant, event);
        return p.getAlias();
    }

    private com.ping.participant.Participant owned(long event, long participant) {
        var p = participants.findById(participant).orElseThrow(() -> new NotFoundException("No encontramos a esa persona."));
        if (p.getEvent().getId() != event) throw new NotFoundException("Esa persona no participa en este evento.");
        return p;
    }
    private void history(long participant, long event, String action, long actor, String reason) {
        jdbc.update("insert into participant_withdrawals(participant_id,event_id,action,actor_id,reason,created_at) values(?,?,?,?,?,?)",
                participant, event, action, actor, reason, Timestamp.from(Instant.now()));
    }
}
