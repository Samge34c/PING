package com.ping.interest;

import jakarta.persistence.*;

@Entity @Table(name = "interests")
public class Interest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 80) private String name;
    @Column(nullable = false) private boolean demo;
    @ManyToOne(fetch=FetchType.EAGER) @JoinColumn(name="category_id") private InterestCategory category;
    public InterestCategory getCategory(){return category;}
    public boolean isSelectable(){return category==null || category.isActive();}
    void changeCategory(InterestCategory value){category=value;}
    protected Interest() { }
    public Long getId() { return id; }
    public String getName() { return name; }
    public boolean isDemo() { return demo; }
}
