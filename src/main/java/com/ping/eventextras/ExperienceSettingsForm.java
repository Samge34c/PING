package com.ping.eventextras;
import jakarta.validation.constraints.*;
public class ExperienceSettingsForm {
 @NotNull @PositiveOrZero private Long version=0L;
 @NotBlank @Size(max=64) private String zoneId="America/Bogota";
 @NotNull @Size(max=240) private String staffLocation="";
 @NotNull @Size(max=240) private String officialContact="";
 private boolean challengeEnabled;
 public Long getVersion(){return version;} public void setVersion(Long v){version=v;}
 public String getZoneId(){return zoneId;} public void setZoneId(String v){zoneId=v;}
 public String getStaffLocation(){return staffLocation;} public void setStaffLocation(String v){staffLocation=v;}
 public String getOfficialContact(){return officialContact;} public void setOfficialContact(String v){officialContact=v;}
 public boolean isChallengeEnabled(){return challengeEnabled;} public void setChallengeEnabled(boolean v){challengeEnabled=v;}
}
