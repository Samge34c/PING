package com.ping.match;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
@Profile("!bootstrap")
public interface CompletionStepRepository extends JpaRepository<CompletionStep, Long> {
    List<CompletionStep> findByMatchIdOrderByIdAsc(long matchId);
    interface CreditTotal { Long getEventId(); Long getTotal(); }
    /** Net points credited per event: each recorded grant at its frozen amount, minus each recorded reversal. */
    @Query("""
        select m.event.id as eventId,
               sum(case when s.action in ('GRANT_FIRST','GRANT_SECOND') then c.rewardPoints else -c.rewardPoints end) as total
        from CompletionStep s, MatchCompletion c, Match m
        where c.matchId = s.matchId and m.id = s.matchId
          and s.action in ('GRANT_FIRST','GRANT_SECOND','REVERSE_FIRST','REVERSE_SECOND')
        group by m.event.id""")
    List<CreditTotal> creditedByEvent();
}
