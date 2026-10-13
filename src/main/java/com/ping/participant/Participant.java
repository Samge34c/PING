package com.ping.participant;

import java.time.Instant;
import java.util.*;
import com.ping.interest.Interest;
import jakarta.persistence.*;
import com.ping.event.Event;

@Entity @Table(name = "participants")
public class Participant {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 80) private String alias;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "event_id", nullable = false) private Event event;
    @Column(nullable = false) private boolean available;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "waiting_since") private Instant waitingSince;
    @Column(nullable = false) private long points;
    public Instant getWaitingSince() { return waitingSince; }
    public long getPoints() { return points; }
    public void rewardCompletedMatch() { points = Math.addExact(points, 10); }
    public void reverseCompletionReward() {
        if (points < 10) throw new IllegalStateException("Cannot reverse a reward without its balance");
        points -= 10;
    }
    public void spendPoints(long cost) {
        if (cost <= 0 || points < cost) throw new com.ping.web.BusinessRuleException("No tienes puntos suficientes para este beneficio.");
        points -= cost;
    }
    public void restartWaiting() { waitingSince = available ? Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS) : null; }
    @Version private long version;
    @ManyToMany
    @JoinTable(name = "participant_interests", joinColumns = @JoinColumn(name = "participant_id"), inverseJoinColumns = @JoinColumn(name = "interest_id"))
    private Set<Interest> interests = new LinkedHashSet<>();
    protected Participant() { }
    public Participant(String alias, Event event, boolean available) { this.alias = alias; this.event = event; this.available = available; }
    @PrePersist void onCreate() { createdAt = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS); if (available) waitingSince = createdAt; }
    public Long getId() { return id; }
    public String getAlias() { return alias; }
    public Event getEvent() { return event; }
    public boolean isAvailable() { return available; }
    public Instant getCreatedAt() { return createdAt; }
    public long getVersion() { return version; }
    public Set<Interest> getInterests() { return Collections.unmodifiableSet(interests); }
    public void replaceInterests(Collection<Interest> selected) { interests.clear(); interests.addAll(selected); }
    void changeAvailability(boolean available) { if (this.available != available) { this.available = available; restartWaiting(); } }
}
