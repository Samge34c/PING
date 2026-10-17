package com.ping.encounter;
import jakarta.validation.constraints.*;
public class PointForm {
 @NotBlank @Size(max=160) private String name="";
 public String getName(){return name;} public void setName(String value){name=value;}
 @NotBlank(message="Escribe indicaciones para encontrar este punto público.") @Size(max=500) private String directions="";
 public String getDirections(){return directions;} public void setDirections(String value){directions=value;}
  private boolean active=true;
 public boolean isActive(){return active;} public void setActive(boolean value){active=value;}
 @PositiveOrZero private Long version=null;
 public Long getVersion(){return version;} public void setVersion(Long value){version=value;}
 public static PointForm from(MeetingPoint e){var f=new PointForm();f.setName(e.getName());f.setDirections(e.getDirections());f.setActive(e.isActive());f.setVersion(e.getVersion());return f;}
}
