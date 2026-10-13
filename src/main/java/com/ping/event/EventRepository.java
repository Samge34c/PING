package com.ping.event;

import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

@Profile("!bootstrap")
public interface EventRepository extends JpaRepository<Event, Long> { }
