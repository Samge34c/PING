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
    interface Totals { Long getEventId(); long getTotal(); long getAvailable(); }
    @Query("select p.event.id as eventId, count(p) as total, sum(case when p.available = true then 1 else 0 end) as available from Participant p group by p.event.id")
    List<Totals> eventTotals();
    interface FreeTotal { Long getEventId(); long getTotal(); }
    @Query("select p.event.id as eventId, count(p) as total from Participant p where p.available = true and not exists (select r from MatchReservation r where r.participantId = p.id) group by p.event.id")
    List<FreeTotal> freeTotals();
    @EntityGraph(attributePaths = {"event", "interests"}) List<Participant> findByEventIdOrderByIdAsc(Long eventId);
    @Override @EntityGraph(attributePaths = {"event", "interests"}) Optional<Participant> findById(Long id);
}
