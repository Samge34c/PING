package com.ping.match;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
@Profile("!bootstrap")
public interface MatchReservationRepository extends JpaRepository<MatchReservation, Long> {
    void deleteByMatchId(long matchId);
}
