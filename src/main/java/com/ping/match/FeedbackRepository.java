package com.ping.match;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
@Profile("!bootstrap")
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    interface Total { Long getEventId(); Feedback.Rating getRating(); long getTotal(); }
    @org.springframework.data.jpa.repository.Query("select f.match.event.id as eventId, f.rating as rating, count(f) as total from Feedback f group by f.match.event.id, f.rating")
    java.util.List<Total> eventTotals();
    Optional<Feedback> findByMatchIdAndParticipantId(long matchId, long participantId);
}
