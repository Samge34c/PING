package com.ping.participant;

import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.*;

@Profile("!bootstrap")
public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    @EntityGraph(attributePaths = {"event", "interests"}) List<Participant> findAllByOrderByCreatedAtDescIdDesc();
    @Override @EntityGraph(attributePaths = {"event", "interests"}) Optional<Participant> findById(Long id);
}
