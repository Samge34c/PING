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
    List<Match> forParticipant(@Param("id") long id);
    long countByStatus(MatchStatus status);
}
