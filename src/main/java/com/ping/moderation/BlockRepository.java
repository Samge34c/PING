package com.ping.moderation;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
@Profile("!bootstrap")
public interface BlockRepository extends JpaRepository<ParticipantBlock, Long> {
    boolean existsByBlockerIdAndBlockedId(long blocker, long blocked);
    @Query("select case when b.blockerId = :self then b.blockedId else b.blockerId end from ParticipantBlock b where b.blockerId = :self or b.blockedId = :self")
    List<Long> excluded(@Param("self") long self);
}
