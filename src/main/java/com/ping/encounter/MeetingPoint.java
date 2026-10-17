package com.ping.encounter;
import jakarta.persistence.*;
@Entity @Table(name="event_meeting_points")
public class MeetingPoint {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private long eventId;
 @Column(nullable=false) private boolean active=true;
 @Version private long version;
 protected MeetingPoint(){}
 MeetingPoint(long eventId){this.eventId=eventId;}
 public Long getId(){return id;} public long getEventId(){return eventId;} public boolean isActive(){return active;} public long getVersion(){return version;}
 @Column(nullable=false,length=160) private String name;
 @Column(nullable=false,length=500) private String directions;
 public String getName(){return name;} public String getDirections(){return directions;}
 void change(PointForm f){name=f.getName().strip();directions=f.getDirections().strip();active=f.isActive();}
}
