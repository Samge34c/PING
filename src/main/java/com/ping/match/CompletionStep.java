package com.ping.match;
import java.time.Instant;
import jakarta.persistence.*;
@Entity @Table(name = "match_completion_steps")
public class CompletionStep {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private long matchId;
    @Column(nullable = false) private int attempt;
    @Column(nullable = false, length = 32) private String action;
    @Column(nullable = false) private Instant createdAt;
    protected CompletionStep() { }
    CompletionStep(MatchCompletion saga, String action) {
        matchId = saga.getMatchId(); attempt = saga.getAttempt(); this.action = action; createdAt = Instant.now();
    }
    public String getAction() { return action; }
    public int getAttempt() { return attempt; }
}
