package com.ping.match;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ping.web.NotFoundException;

/** A single consistent read of the public state signature, independent of history size. */
@Service @Profile("!bootstrap") @Transactional(readOnly = true)
public class MatchLiveState {
    private final JdbcTemplate jdbc;
    public MatchLiveState(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Map<String, String> forParticipant(long self) {
        var rows = jdbc.query("""
                select p.id, p.version, e.status event_status,
                       m.id match_id, m.status match_status, m.first_id,
                       m.first_accepted, m.second_accepted, m.first_confirmed, m.second_confirmed,
                       m.completion_pending, m.first_arrived, m.second_arrived,
                       m.first_verified, m.second_verified, other.version other_version,
                       case when m.status='ACTIVE' and m.verification_policy>=2 then
                         (select count(*) from purpose_credentials c
                          where c.purpose='ENCOUNTER' and c.context_id=m.id and c.issuer_id=p.id)
                         else 0 end code_revision
                from participants p join events e on e.id=p.event_id
                left join ping_matches m on m.id=(
                    select max(recent.id) from ping_matches recent
                    where (recent.first_id=p.id or recent.second_id=p.id)
                      and recent.status in ('OFFERED','ACTIVE'))
                left join participants other on other.id=case
                    when m.first_id=p.id then m.second_id else m.first_id end
                where p.id=?
                """, (r, n) -> {
                    var state = new LinkedHashMap<String, String>();
                    String signature = self + ":" + r.getLong("version") + ":" + r.getString("event_status") + ":";
                    Long matchId = r.getObject("match_id", Long.class);
                    String status = matchId == null ? "NONE" : r.getString("match_status");
                    if (matchId == null) signature += "waiting";
                    else {
                        boolean first = r.getLong("first_id") == self;
                        String own = first ? "first_" : "second_", other = first ? "second_" : "first_";
                        signature += matchId + ":" + status + ":" + r.getBoolean(own + "accepted") + ":"
                                + r.getBoolean(other + "accepted") + ":" + r.getBoolean(own + "confirmed") + ":"
                                + r.getBoolean(other + "confirmed") + ":" + r.getBoolean("completion_pending") + ":"
                                + r.getBoolean(own + "arrived") + ":" + r.getBoolean(other + "arrived") + ":"
                                + r.getBoolean(own + "verified") + ":" + r.getBoolean(other + "verified") + ":"
                                + r.getLong("code_revision");
                        if (status.equals("ACTIVE") && r.getBoolean("first_accepted") && r.getBoolean("second_accepted"))
                            state.put("recognitionRevision", matchId + ":" + r.getLong("other_version"));
                    }
                    state.put("signature", signature);
                    state.put("matchStatus", status);
                    return (Map<String, String>) state;
                }, self);
        if (rows.isEmpty()) throw new NotFoundException("No encontramos ese participante.");
        return rows.getFirst();
    }
}
