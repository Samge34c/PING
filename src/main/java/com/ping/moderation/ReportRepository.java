package com.ping.moderation;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
@Profile("!bootstrap")
public interface ReportRepository extends JpaRepository<InteractionReport, Long> {
    Optional<InteractionReport> findByMatchIdAndReporterId(long match, long self);
    @EntityGraph(attributePaths = {"match", "match.first", "match.second"})
    @Query("select r from InteractionReport r where r.match.event.id = :eventId order by r.id desc")
    List<InteractionReport> forEvent(@Param("eventId") long eventId);
}
