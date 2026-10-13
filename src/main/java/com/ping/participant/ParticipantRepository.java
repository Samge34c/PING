package com.ping.participant;

import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.*;

@Profile("!bootstrap")
public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    @EntityGraph(attributePaths = {"event", "interests"}) List<Participant> findAllByOrderByCreatedAtDescIdDesc();
    @org.springframework.data.jpa.repository.Query("select p.event.id from Participant p where p.id = :id")
    Optional<Long> eventId(@org.springframework.data.repository.query.Param("id") long id);
    @EntityGraph(attributePaths = {"event", "interests"}) List<Participant> findByEventIdAndAvailableTrue(Long eventId);
    @Override @EntityGraph(attributePaths = {"event", "interests"}) Optional<Participant> findById(Long id);
}
