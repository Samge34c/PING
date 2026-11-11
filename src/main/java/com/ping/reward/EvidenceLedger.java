package com.ping.reward;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/**
 * DEC-068. Extends the single balance (participants.points); it is not a second wallet.
 * Points granted by an AT_DELIVERY encounter are ordinary, spendable points that are also recorded here as
 * "to check at delivery" until one in-person validation of that encounter. A receipt spends checked points first;
 * whatever part it pays with unchecked points is linked to the encounter, and that receipt cannot be delivered
 * before the evidence is validated. Callers already hold the event lock.
 */
@Service @Profile("!bootstrap") @Transactional(propagation = Propagation.MANDATORY)
public class EvidenceLedger {
    public record Requirement(long matchId, String activityTitle, String evidence, String firstAlias, String secondAlias, int amount,
                              Instant validatedAt, String validatedBy, Long validatedByAccount) {
        public boolean validated() { return validatedAt != null; }
    }
    public record Note(String note, Instant createdAt) { }
    private final JdbcTemplate jdbc;
    public EvidenceLedger(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    /** Saga step: record the credit once (primary key match+participant). */
    public void credit(long match, long participant, int amount) {
        if (amount <= 0) return;
        if (jdbc.queryForObject("select count(*) from evidence_credits where match_id=? and participant_id=?", Integer.class, match, participant) > 0) return;
        jdbc.update("insert into evidence_credits(match_id,participant_id,amount,unspent,created_at) values(?,?,?,?,?)", match, participant, amount, amount, Timestamp.from(Instant.now()));
    }
    /** Saga compensation: the same credit disappears with its points (it cannot have been spent: no receipt while completion is pending). */
    public void uncredit(long match, long participant) { jdbc.update("delete from evidence_credits where match_id=? and participant_id=?", match, participant); }

    /** Points of this participant still waiting for an evidence check (unspent and not validated). */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public long unchecked(long participant) {
        return jdbc.queryForObject("select coalesce(sum(c.unspent),0) from evidence_credits c left join evidence_validations v on v.match_id=c.match_id where c.participant_id=? and v.match_id is null", Long.class, participant);
    }

    /**
     * Called with the balance before participant.spendPoints(cost). Order: ordinary points first, then credits of encounters whose
     * evidence is already validated (linked only so staff and the participant see «ya validada»), then the oldest unchecked credits,
     * which block delivery until validated.
     */
    public void allocate(long claim, long participant, long balanceBefore, long cost) {
        var open = jdbc.queryForList("select c.match_id, c.unspent from evidence_credits c left join evidence_validations v on v.match_id=c.match_id where c.participant_id=? and c.unspent>0 order by case when v.match_id is null then 1 else 0 end, c.created_at, c.match_id", participant);
        long linked = open.stream().mapToLong(r -> ((Number) r.get("unspent")).longValue()).sum();
        long rest = cost - Math.max(0, Math.min(cost, balanceBefore - linked));
        for (var row : open) {
            if (rest <= 0) break;
            long match = ((Number) row.get("match_id")).longValue(); int take = (int) Math.min(rest, ((Number) row.get("unspent")).longValue());
            jdbc.update("update evidence_credits set unspent=unspent-? where match_id=? and participant_id=?", take, match, participant);
            jdbc.update("insert into claim_evidence(claim_id,match_id,amount) values(?,?,?)", claim, match, take);
            rest -= take;
        }
        if (rest > 0) throw new IllegalStateException("Evidence allocation exceeds the balance");
    }

    /** A cancelled (refunded) receipt gives its unchecked part back to the same credits. */
    public void release(long claim, long participant) {
        for (var row : jdbc.queryForList("select match_id, amount from claim_evidence where claim_id=?", claim))
            jdbc.update("update evidence_credits set unspent=unspent+? where match_id=? and participant_id=?", row.get("amount"), row.get("match_id"), participant);
        jdbc.update("delete from claim_evidence where claim_id=?", claim);
    }

    /** What this receipt requires (validated or not), with what to show. */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public List<Requirement> requirements(long claim) {
        return jdbc.query("""
                select e.match_id, e.amount, m.activity_title, m.check_instructions, f.alias first_alias, s.alias second_alias, v.validated_at, a.email, v.validated_by
                from claim_evidence e join ping_matches m on m.id=e.match_id join participants f on f.id=m.first_id join participants s on s.id=m.second_id
                left join evidence_validations v on v.match_id=e.match_id left join ping_accounts a on a.id=v.validated_by
                where e.claim_id=? order by e.match_id""",
                (r, n) -> new Requirement(r.getLong(1), r.getString(3), r.getString(4), r.getString(5), r.getString(6), r.getInt(2),
                        r.getTimestamp(7) == null ? null : r.getTimestamp(7).toInstant(), r.getString(8), r.getObject(9) == null ? null : r.getLong(9)), claim);
    }
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public boolean missing(long claim) { return requirements(claim).stream().anyMatch(r -> !r.validated()); }

    /** One validation per encounter (primary key). Repeating it is a no-op; it never grants points. */
    public boolean validate(long claim, long match, long event, long staff) {
        if (jdbc.queryForObject("select count(*) from claim_evidence where claim_id=? and match_id=?", Integer.class, claim, match) == 0)
            throw new com.ping.web.BusinessRuleException("Este recibo no necesita esa evidencia.");
        if (jdbc.queryForObject("select count(*) from evidence_validations where match_id=?", Integer.class, match) > 0) return false;
        jdbc.update("insert into evidence_validations(match_id,event_id,validated_by,validated_at,claim_id) values(?,?,?,?,?)", match, event, staff, Timestamp.from(Instant.now()), claim);
        return true;
    }
    public void note(long claim, long staff, String note) {
        jdbc.update("insert into claim_evidence_notes(claim_id,staff_id,note,created_at) values(?,?,?,?)", claim, staff, note, Timestamp.from(Instant.now()));
    }
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public Optional<Note> lastNote(long claim) {
        return jdbc.query("select note, created_at from claim_evidence_notes where claim_id=? order by id desc", (r, n) -> new Note(r.getString(1), r.getTimestamp(2).toInstant()), claim).stream().findFirst();
    }
    /** Whether the in-person check of this encounter is already registered (covers both participants). */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public boolean matchValidated(long match) { return jdbc.queryForObject("select count(*) from evidence_validations where match_id=?", Integer.class, match) > 0; }
}
