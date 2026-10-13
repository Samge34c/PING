package com.ping.event;

import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

@Profile("!bootstrap")
public interface EventRepository extends JpaRepository<Event, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select e from Event e where e.id = :id")
    java.util.Optional<Event> lockById(@org.springframework.data.repository.query.Param("id") long id);
}
