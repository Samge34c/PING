package com.ping.reward;
import jakarta.persistence.*;
@Entity @Table(name = "benefits")
public class Benefit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 120) private String name;
    @Column(nullable = false) private long pointsCost;
    @Column(nullable = false) private boolean active;
    protected Benefit() { }
    public Long getId() { return id; }
    public String getName() { return name; }
    public long getPointsCost() { return pointsCost; }
    public boolean isActive() { return active; }
}
