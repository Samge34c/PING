package com.ping.reward;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
@Profile("!bootstrap")
public interface BenefitRepository extends JpaRepository<Benefit, Long> {
    List<Benefit> findByActiveTrueOrderByIdAsc();
}
