package com.ping.encounter;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
@Profile("!bootstrap") public interface MeetingPointRepository extends JpaRepository<MeetingPoint,Long> {List<MeetingPoint> findByEventIdOrderByIdAsc(long eventId);}
