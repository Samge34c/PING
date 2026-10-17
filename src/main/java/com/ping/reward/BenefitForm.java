package com.ping.reward;
import jakarta.validation.constraints.*;
public class BenefitForm {
 @NotBlank @Size(max=120) private String name="";
 @Size(max=2000) private String description="";
 @Min(1) @Max(10000) private long pointsCost=20;
 @NotBlank @Size(max=160) private String claimLocation="";
 @NotBlank @Size(max=500) private String claimInstructions="";
 private boolean active=true;
 @PositiveOrZero private Long version;
 public String getName(){return name;}public void setName(String v){name=v;}
 public String getDescription(){return description;}public void setDescription(String v){description=v;}
 public long getPointsCost(){return pointsCost;}public void setPointsCost(long v){pointsCost=v;}
 public String getClaimLocation(){return claimLocation;}public void setClaimLocation(String v){claimLocation=v;}
 public String getClaimInstructions(){return claimInstructions;}public void setClaimInstructions(String v){claimInstructions=v;}
 public boolean isActive(){return active;}public void setActive(boolean v){active=v;}
 public Long getVersion(){return version;}public void setVersion(Long v){version=v;}
 public static BenefitForm from(Benefit b){var f=new BenefitForm();f.name=b.getName();f.description=b.getDescription();f.pointsCost=b.getPointsCost();f.claimLocation=b.getClaimLocation();f.claimInstructions=b.getClaimInstructions();f.active=b.isActive();f.version=b.getVersion();return f;}
}
