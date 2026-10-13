package com.ping.interest;

import jakarta.persistence.*;

@Entity @Table(name = "interests")
public class Interest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 80) private String name;
    @Column(nullable = false) private boolean demo;
    protected Interest() { }
    public Long getId() { return id; }
    public String getName() { return name; }
    public boolean isDemo() { return demo; }
}
