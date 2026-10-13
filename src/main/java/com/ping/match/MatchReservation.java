package com.ping.match;
import jakarta.persistence.*;
@Entity @Table(name = "match_reservations")
public class MatchReservation {
    @Id @Column(name = "participant_id") private Long participantId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "match_id") private Match match;
    protected MatchReservation() { }
    MatchReservation(long participantId, Match match) { this.participantId = participantId; this.match = match; }
    public Long getParticipantId() { return participantId; }
}
