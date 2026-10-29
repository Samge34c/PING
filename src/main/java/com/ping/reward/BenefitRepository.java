package com.ping.reward;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
@Profile("!bootstrap")
public interface BenefitRepository extends JpaRepository<Benefit, Long> {
    List<Benefit> findByActiveTrueOrderByIdAsc();
    List<Benefit> findByActiveTrueAndEventIdIsNullOrderByIdAsc();
    @org.springframework.data.jpa.repository.Query("select b from Benefit b where b.active=true and (b.eventId is null or b.eventId=:event) order by b.id")
    List<Benefit> catalogForEvent(@org.springframework.data.repository.query.Param("event") long event);
    List<Benefit> findByEventIdOrderByIdAsc(long event);
}
