package com.ping.match;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
@Profile("!bootstrap")
public interface MatchRepository extends JpaRepository<Match, Long> {
    @Query("select m.event.id from Match m where m.id = :id") Optional<Long> eventId(@Param("id") long id);
    List<Match> findByEventIdAndStatus(long eventId, MatchStatus status);
    @Query("select m from Match m where m.first.id = :id or m.second.id = :id order by m.id desc")
    @EntityGraph(attributePaths = {"first", "second", "event"})
    List<Match> forParticipant(@Param("id") long id);
    interface Total { Long getEventId(); MatchStatus getStatus(); long getTotal(); }
    @Query("select m.event.id as eventId, m.status as status, count(m) as total from Match m group by m.event.id, m.status")
    List<Total> eventTotals();
    @EntityGraph(attributePaths = {"first", "second", "event"})
    List<Match> findByEventIdOrderByIdDesc(long eventId);
    @Query("select case when m.first.id=:self then m.second.id else m.first.id end from Match m where m.event.id=:event and m.status=com.ping.match.MatchStatus.COMPLETED and(m.first.id=:self or m.second.id=:self)")
    List<Long> successfulPartners(@Param("event")long event,@Param("self")long self);
    long countByStatus(MatchStatus status);
}
