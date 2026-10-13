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
    @Version private long version;
    @ManyToMany
    @JoinTable(name = "participant_interests", joinColumns = @JoinColumn(name = "participant_id"), inverseJoinColumns = @JoinColumn(name = "interest_id"))
    private Set<Interest> interests = new LinkedHashSet<>();
    protected Participant() { }
    public Participant(String alias, Event event, boolean available) { this.alias = alias; this.event = event; this.available = available; }
    @PrePersist void onCreate() { createdAt = Instant.now(); }
    public Long getId() { return id; }
    public String getAlias() { return alias; }
    public Event getEvent() { return event; }
    public boolean isAvailable() { return available; }
    public Instant getCreatedAt() { return createdAt; }
    public long getVersion() { return version; }
    public Set<Interest> getInterests() { return Collections.unmodifiableSet(interests); }
    public void replaceInterests(Collection<Interest> selected) { interests.clear(); interests.addAll(selected); }
    void changeAvailability(boolean available) { this.available = available; }
}
