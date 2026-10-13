package com.ping.demo;
import jakarta.persistence.*;
import com.ping.event.Event;
@Entity @Table(name = "demo_scenarios")
public class DemoScenario {
    @Id private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "event_id") private Event event;
    protected DemoScenario() { }
    public Event getEvent() { return event; }
    void setEvent(Event event) { this.event = event; }
}
