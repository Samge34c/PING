package com.ping.match;

import java.time.Instant;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.ping.event.Event;
import com.ping.participant.Participant;

@Entity @Table(name = "ping_matches")
public class Match {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "event_id") private Event event;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "first_id") private Participant first;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "second_id") private Participant second;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR) @Column(nullable = false, length = 16) private MatchStatus status = MatchStatus.OFFERED;
    private boolean firstAccepted, secondAccepted, firstConfirmed, secondConfirmed;
    @Column(nullable = false) private Instant firstWait;
    @Column(nullable = false) private Instant secondWait;
    @Column(nullable = false, length = 700) private String commonInterests;
    @Column(nullable = false, length = 700) private String firstInterests;
    @Column(nullable = false, length = 700) private String secondInterests;
    @Column(length = 160) private String meetingPoint;
    @Column(length = 80, unique = true) private String word;
    @Column(length = 500) private String activity;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private boolean completionPending;
    public boolean isCompletionPending() { return completionPending; }
    void prepareCompletion() { completionPending = true; }
    void restoreCompletion(long trigger) {
        completionPending = false; status = MatchStatus.ACTIVE;
        if (first.getId() == trigger) firstConfirmed = false; else secondConfirmed = false;
    }
    @Version private long version;
    protected Match() { }
    Match(Event event, Participant first, Participant second, String common, String firstInterests, String secondInterests) {
        this.event = event; this.first = first; this.second = second;
        this.commonInterests = common; this.firstInterests = firstInterests; this.secondInterests = secondInterests;
        firstWait = first.getWaitingSince(); secondWait = second.getWaitingSince(); createdAt = Instant.now();
    }
    public Long getId() { return id; }
    public Event getEvent() { return event; }
    public Participant getFirst() { return first; }
    public Participant getSecond() { return second; }
    public Instant getCreatedAt() { return createdAt; }
    public MatchStatus getStatus() { return status; }
    public String getCommonInterests() { return commonInterests; }
    public String getFirstInterests() { return firstInterests; }
    public String getSecondInterests() { return secondInterests; }
    public String getMeetingPoint() { return meetingPoint; }
    public String getWord() { return word; }
    public String getActivity() { return activity; }
    public boolean owns(long id) { return first.getId() == id || second.getId() == id; }
    public boolean accepted(long id) { return first.getId() == id ? firstAccepted : secondAccepted; }
    public boolean confirmed(long id) { return first.getId() == id ? firstConfirmed : secondConfirmed; }
    void accept(long id) { if (first.getId() == id) firstAccepted = true; else secondAccepted = true; }
    boolean bothAccepted() { return firstAccepted && secondAccepted; }
    void confirm(long id) { if (first.getId() == id) firstConfirmed = true; else secondConfirmed = true; }
    boolean bothConfirmed() { return firstConfirmed && secondConfirmed; }
    void activate(String word, String activity) { status = MatchStatus.ACTIVE; this.word = word; this.activity = activity; meetingPoint = event.getMeetingPoint(); }
    void complete() { status = MatchStatus.COMPLETED; completionPending = false; }
    void cancel() { status = MatchStatus.CANCELLED; }
    boolean excludedPair(Participant a, Participant b) {
        if (status != MatchStatus.CANCELLED) return false;
        // Database timestamps have microsecond precision; normalize before comparing.
        return sameCycle(first, firstWait, a, b) && sameCycle(second, secondWait, a, b);
    }
    private boolean sameCycle(Participant original, Instant waiting, Participant a, Participant b) {
        Participant current = original.getId().equals(a.getId()) ? a : original.getId().equals(b.getId()) ? b : null;
        return current != null && current.getWaitingSince() != null && waiting.equals(current.getWaitingSince());
    }
}
