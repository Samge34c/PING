package com.ping.encounter;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
@Profile("!bootstrap") public interface EventActivityRepository extends JpaRepository<EventActivity,Long> {List<EventActivity> findByEventIdOrderByIdAsc(long eventId);}
