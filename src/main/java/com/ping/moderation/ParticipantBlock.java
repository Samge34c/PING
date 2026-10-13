package com.ping.moderation;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name = "participant_blocks", uniqueConstraints = @UniqueConstraint(columnNames = {"blocker_id", "blocked_id"}))
public class ParticipantBlock {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "blocker_id", nullable = false) private long blockerId;
    @Column(name = "blocked_id", nullable = false) private long blockedId;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    protected ParticipantBlock() { }
    ParticipantBlock(long blocker, long blocked) { blockerId = blocker; blockedId = blocked; createdAt = Instant.now(); }
}
