package com.ping.demo;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.*;
@Profile("!bootstrap")
public interface DemoScenarioRepository extends JpaRepository<DemoScenario, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select d from DemoScenario d where d.id = 1")
    Optional<DemoScenario> lockScenario();
}
