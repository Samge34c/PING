package com.ping.reward;
import jakarta.persistence.*;
import com.ping.participant.Participant;
import java.time.Instant;
@Entity @Table(name = "reward_claims", uniqueConstraints = @UniqueConstraint(columnNames = {"participant_id", "request_key"}))
public class RewardClaim {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "participant_id") private Participant participant;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "benefit_id") private Benefit benefit;
    @Column(nullable = false, length = 36) private String requestKey;
    @Column(nullable = false) private long cost;
    @Enumerated(EnumType.STRING) @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(nullable = false, length = 16) private ClaimStatus status;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable=false,length=120) private String benefitName;
    @Column(nullable=false,length=2000) private String benefitDescription;
    @Column(nullable=false,length=160) private String claimLocation;
    @Column(nullable=false,length=500) private String claimInstructions;
    public String getBenefitName(){return benefitName;} public String getBenefitDescription(){return benefitDescription;}
    public String getClaimLocation(){return claimLocation;} public String getClaimInstructions(){return claimInstructions;}
    protected RewardClaim() { }
    RewardClaim(Participant participant, Benefit benefit, String key) {
        this.participant = participant; this.benefit = benefit;
        benefitName=benefit.getName();benefitDescription=benefit.getDescription();claimLocation=benefit.getClaimLocation();claimInstructions=benefit.getClaimInstructions(); requestKey = key; cost = benefit.getPointsCost(); status = ClaimStatus.REQUESTED; createdAt = Instant.now();
    }
    void defaultDelivery(com.ping.event.Event e){if(e.getDeliveryName().isBlank()||e.getDeliveryDirections().isBlank())throw new com.ping.web.BusinessRuleException("El organizador debe configurar el lugar de entrega.");claimLocation=e.getDeliveryName();claimInstructions=e.getDeliveryDirections();}
    public Long getId() { return id; }
    public Participant getParticipant() { return participant; }
    public Benefit getBenefit() { return benefit; }
    public long getCost() { return cost; }
    public ClaimStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    void finish(ClaimStatus target) { status = target; }
}
