package com.ping.match;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
@Profile("!bootstrap")
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    Optional<Feedback> findByMatchIdAndParticipantId(long matchId, long participantId);
}
