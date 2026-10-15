package com.ping.reward;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
@Profile("!bootstrap")
public interface RewardClaimRepository extends JpaRepository<RewardClaim, Long> {
    Optional<RewardClaim> findByParticipantIdAndRequestKey(long participantId, String requestKey);
    @EntityGraph(attributePaths = {"participant", "benefit"})
    List<RewardClaim> findByParticipantIdOrderByIdDesc(long participantId);
    @EntityGraph(attributePaths = {"participant", "benefit"})
    @Query("select c from RewardClaim c where c.participant.event.id = :eventId order by c.id desc")
    List<RewardClaim> forEvent(@Param("eventId") long eventId);
    @Query("select c.participant.event.id from RewardClaim c where c.id = :id")
    Optional<Long> eventId(@Param("id") long id);
}
