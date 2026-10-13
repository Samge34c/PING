package com.ping.match;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
@Profile("!bootstrap")
public interface CompletionStepRepository extends JpaRepository<CompletionStep, Long> {
    List<CompletionStep> findByMatchIdOrderByIdAsc(long matchId);
}
