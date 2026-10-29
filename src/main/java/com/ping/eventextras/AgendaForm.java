package com.ping.eventextras;
import jakarta.validation.constraints.*;
public class AgendaForm {
 @NotNull @PositiveOrZero private Long version=0L;
 @NotBlank @Size(max=36) private String requestKey=java.util.UUID.randomUUID().toString();
 @NotBlank @Size(max=120) private String title="";
 @NotNull @Size(max=500) private String description="";
 @NotBlank @Size(max=32) private String startsAt="";
 @NotNull @Size(max=32) private String endsAt="";
 @NotBlank @Size(max=64) private String zoneId="America/Bogota";
 @NotNull @Size(max=160) private String place="";
 private Long pointId;
 private boolean active=true;
 public Long getVersion(){return version;} public void setVersion(Long v){version=v;}
 public String getRequestKey(){return requestKey;} public void setRequestKey(String v){requestKey=v;}
 public String getTitle(){return title;} public void setTitle(String v){title=v;}
 public String getDescription(){return description;} public void setDescription(String v){description=v;}
 public String getStartsAt(){return startsAt;} public void setStartsAt(String v){startsAt=v;}
 public String getEndsAt(){return endsAt;} public void setEndsAt(String v){endsAt=v;}
 public String getZoneId(){return zoneId;} public void setZoneId(String v){zoneId=v;}
 public String getPlace(){return place;} public void setPlace(String v){place=v;}
 public Long getPointId(){return pointId;} public void setPointId(Long v){pointId=v;}
 public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
}
