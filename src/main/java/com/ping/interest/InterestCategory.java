package com.ping.interest;
import jakarta.persistence.*;
@Entity @Table(name="interest_categories")
public class InterestCategory {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,unique=true,length=80) private String name;
 @Column(nullable=false) private int presentationOrder;
 @Column(nullable=false) private boolean active=true;
 @Version private long version;
 protected InterestCategory(){}
 public Long getId(){return id;} public String getName(){return name;}
 public int getPresentationOrder(){return presentationOrder;} public boolean isActive(){return active;} public long getVersion(){return version;}
 void change(String name,int order,boolean active){this.name=name;presentationOrder=order;this.active=active;}
}
