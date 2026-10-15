package com.ping.moderation;
import com.ping.match.Match;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name = "interaction_reports", uniqueConstraints = @UniqueConstraint(columnNames = {"match_id", "reporter_id"}))
public class InteractionReport {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "match_id") private Match match;
    @Column(name = "reporter_id", nullable = false) private long reporterId;
    @Enumerated(EnumType.STRING) @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(nullable = false, length = 24) private ReportReason reason;
    @Column(nullable = false, length = 300) private String comment;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    protected InteractionReport() { }
    InteractionReport(Match match, long self, ReportReason reason, String comment) {
        this.match = match; reporterId = self; this.reason = reason; this.comment = comment; createdAt = Instant.now();
    }
    public Match getMatch() { return match; }
    public long getReporterId() { return reporterId; }
    public ReportReason getReason() { return reason; }
    public String getComment() { return comment; }
    public Instant getCreatedAt() { return createdAt; }
}
