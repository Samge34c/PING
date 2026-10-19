package com.ping.reward;
import jakarta.persistence.*;
@Entity @Table(name = "benefits")
public class Benefit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 120) private String name;
    @Column(nullable = false) private long pointsCost;
    @Column(nullable = false) private boolean active;
    @Column private Long eventId;
    @Column(nullable=false,length=2000) private String description="";
    @Column(nullable=false,length=160) private String claimLocation="";
    @Column(nullable=false,length=500) private String claimInstructions="";
    @Column(nullable=false) private boolean defaultDelivery;
    public boolean isDefaultDelivery(){return defaultDelivery;}
    @Version private long version;
    protected Benefit() { }
    Benefit(long event){eventId=event;}
    public Long getEventId(){return eventId;} public String getDescription(){return description;}
    public String getClaimLocation(){return claimLocation;} public String getClaimInstructions(){return claimInstructions;} public long getVersion(){return version;}
    void change(BenefitForm f){defaultDelivery=f.isDefaultDelivery();name=f.getName().strip();description=f.getDescription().strip();pointsCost=f.getPointsCost();claimLocation=f.getClaimLocation().strip();claimInstructions=f.getClaimInstructions().strip();active=f.isActive();}
    public Long getId() { return id; }
    public String getName() { return name; }
    public long getPointsCost() { return pointsCost; }
    public boolean isActive() { return active; }
}
