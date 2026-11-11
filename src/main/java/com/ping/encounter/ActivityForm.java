package com.ping.encounter;
import jakarta.validation.constraints.*;
public class ActivityForm {
 @Min(0) @Max(100) private int rewardPoints=10;
 public int getRewardPoints(){return rewardPoints;} public void setRewardPoints(int value){rewardPoints=value;}
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
 @NotNull private CheckMethod verificationMethod=CheckMethod.BILATERAL;
 public CheckMethod getVerificationMethod(){return verificationMethod;} public void setVerificationMethod(CheckMethod value){verificationMethod=value;}
 @Size(max=500) private String checkInstructions="";
 public String getCheckInstructions(){return checkInstructions;} public void setCheckInstructions(String value){checkInstructions=value==null?"":value;}
 @Size(max=200) private String responsePrompt="";
 public String getResponsePrompt(){return responsePrompt;} public void setResponsePrompt(String value){responsePrompt=value==null?"":value;}
 @AssertTrue(message="Explica qué revisará el personal o qué evidencia deberán mostrar al recoger el premio.")
 public boolean isCheckDescribed(){return verificationMethod==null||!verificationMethod.needsInstructions()||!checkInstructions.isBlank();}
 @PositiveOrZero private Long version=null;
 public Long getVersion(){return version;} public void setVersion(Long value){version=value;}
 public static ActivityForm from(EventActivity e){var f=new ActivityForm();f.setRewardPoints(e.getRewardPoints());f.setTitle(e.getTitle());f.setInstructions(e.getInstructions());f.setPointIds(e.getPointIds());f.setInterestIds(e.getInterestIds());f.setActive(e.isActive());f.setVerificationMethod(e.getVerificationMethod());f.setCheckInstructions(e.getCheckInstructions());f.setResponsePrompt(e.getResponsePrompt());f.setVersion(e.getVersion());return f;}
}
