package com.ping.event;

import java.time.Instant;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "events")
public class Event {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "entry_code", nullable = false, length = 32, unique = true, updatable = false)
    private String entryCode;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(nullable = false, length = 2000)
    private String description = "";
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 16)
    private EventStatus status = EventStatus.DRAFT;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Column(name = "meeting_point", nullable = false, length = 160)
    private String meetingPoint = "Punto Ping del evento";
    public String getMeetingPoint() { return meetingPoint; }
    void changeMeetingPoint(String value) { meetingPoint = value; }
    @Version
    private long version;

    protected Event() { }
    public Event(String name, String description) {
        this.entryCode = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(java.util.Locale.ROOT);
        this.name = name;
        this.description = description;
    }
    @PrePersist void onCreate() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
    public Long getId() { return id; }
    public String getEntryCode() { return entryCode; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public EventStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
    public boolean canStartNewMatches() { return status == EventStatus.OPEN; }
    void changeDetails(String name, String description) { this.name = name; this.description = description; }
    void changeStatus(EventStatus status) { this.status = status; }
}
