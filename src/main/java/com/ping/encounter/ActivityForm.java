package com.ping.encounter;
import jakarta.validation.constraints.*;
public class ActivityForm {
 @NotBlank @Size(max=120) private String title="";
 public String getTitle(){return title;} public void setTitle(String value){title=value;}
 @NotBlank @Size(max=500) private String instructions="";
 public String getInstructions(){return instructions;} public void setInstructions(String value){instructions=value;}
 @NotEmpty @Size(max=50) private java.util.Set<Long> pointIds=new java.util.LinkedHashSet<>();
 public java.util.Set<Long> getPointIds(){return pointIds;} public void setPointIds(java.util.Set<Long> value){pointIds=value;}
 @NotNull @Size(max=50) private java.util.Set<Long> interestIds=new java.util.LinkedHashSet<>();
 public java.util.Set<Long> getInterestIds(){return interestIds;} public void setInterestIds(java.util.Set<Long> value){interestIds=value;}
  private boolean active=true;
 public boolean isActive(){return active;} public void setActive(boolean value){active=value;}
 @PositiveOrZero private Long version=null;
 public Long getVersion(){return version;} public void setVersion(Long value){version=value;}
 public static ActivityForm from(EventActivity e){var f=new ActivityForm();f.setTitle(e.getTitle());f.setInstructions(e.getInstructions());f.setPointIds(e.getPointIds());f.setInterestIds(e.getInterestIds());f.setActive(e.isActive());f.setVersion(e.getVersion());return f;}
}
