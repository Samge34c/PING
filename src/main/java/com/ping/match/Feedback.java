package com.ping.match;
import jakarta.persistence.*;
@Entity @Table(name = "feedback", uniqueConstraints = @UniqueConstraint(columnNames = {"match_id", "participant_id"}))
public class Feedback {
    public enum Rating { USEFUL, NEUTRAL, NOT_USEFUL }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "match_id") private Match match;
    @Column(name = "participant_id", nullable = false) private long participantId;
    @Enumerated(EnumType.STRING) @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR) @Column(nullable = false, length = 16) private Rating rating;
    protected Feedback() { }
    Feedback(Match match, long participantId, Rating rating) { this.match = match; this.participantId = participantId; this.rating = rating; }
    public Rating getRating() { return rating; }
}
