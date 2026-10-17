package com.ping.encounter;
import jakarta.persistence.*;
@Entity @Table(name="event_activities")
public class EventActivity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private long eventId;
 @Column(nullable=false) private boolean active=true;
 @Version private long version;
 protected EventActivity(){}
 EventActivity(long eventId){this.eventId=eventId;}
 public Long getId(){return id;} public long getEventId(){return eventId;} public boolean isActive(){return active;} public long getVersion(){return version;}
 @Column(nullable=false,length=120) private String title;
 @Column(nullable=false,length=500) private String instructions;
 @ElementCollection(fetch=FetchType.EAGER) @CollectionTable(name="event_activity_points",joinColumns=@JoinColumn(name="activity_id")) @Column(name="point_id") private java.util.Set<Long> pointIds=new java.util.LinkedHashSet<>();
 @ElementCollection(fetch=FetchType.EAGER) @CollectionTable(name="event_activity_interests",joinColumns=@JoinColumn(name="activity_id")) @Column(name="interest_id") private java.util.Set<Long> interestIds=new java.util.LinkedHashSet<>();
 public String getTitle(){return title;} public String getInstructions(){return instructions;}
 public java.util.Set<Long> getPointIds(){return java.util.Set.copyOf(pointIds);} public java.util.Set<Long> getInterestIds(){return java.util.Set.copyOf(interestIds);}
 void change(ActivityForm f){title=f.getTitle().strip();instructions=f.getInstructions().strip();active=f.isActive();pointIds=new java.util.LinkedHashSet<>(f.getPointIds());interestIds=new java.util.LinkedHashSet<>(f.getInterestIds());}
}
