package com.ping.match;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Entity @Table(name = "match_completion_sagas")
public class MatchCompletion {
    @Id @Column(name = "match_id") private Long matchId;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR) @Column(nullable = false, length = 32) private SagaStatus status;
    @Column(nullable=false,updatable=false) private int rewardPoints=10;
    public int getRewardPoints(){return rewardPoints;}
    @Column(nullable = false) private int attempt;
    @Column(nullable = false) private long triggerParticipantId;
    @Column(nullable = false) private boolean firstGranted, secondGranted;
    @Column(length = 80) private String failureCode;
    protected MatchCompletion() { }
    MatchCompletion(long matchId, long trigger, int points) { this.matchId = matchId;this.rewardPoints=points; restart(trigger); }
    void restart(long trigger) { attempt++; triggerParticipantId = trigger; status = SagaStatus.MATCH_PREPARED; failureCode = null; }
    public Long getMatchId() { return matchId; }
    public SagaStatus getStatus() { return status; }
    public int getAttempt() { return attempt; }
    public long getTriggerParticipantId() { return triggerParticipantId; }
    public boolean isFirstGranted() { return firstGranted; }
    public boolean isSecondGranted() { return secondGranted; }
    public String getFailureCode() { return failureCode; }
    void firstGranted() { firstGranted = true; status = SagaStatus.FIRST_REWARD_GRANTED; }
    void secondGranted() { secondGranted = true; status = SagaStatus.SECOND_REWARD_GRANTED; }
    void reverseFirst() { firstGranted = false; }
    void reverseSecond() { secondGranted = false; }
    void completed() { status = SagaStatus.COMPLETED; }
    void compensating() { status = SagaStatus.COMPENSATING; failureCode = "COMPLETION_STEP_FAILED"; }
    void compensated() { status = SagaStatus.COMPENSATED; }
    void failed() { status = SagaStatus.FAILED; failureCode = "COMPENSATION_RETRY_REQUIRED"; }
}
