package com.ping.match;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
@Profile("!bootstrap")
public interface MatchCompletionRepository extends JpaRepository<MatchCompletion, Long> {
    List<MatchCompletion> findByStatusNotIn(List<SagaStatus> statuses);
}
