package com.ping.event;
import jakarta.validation.constraints.*;
public class EventSectionForm {
 @NotNull @PositiveOrZero private Long version;
 @Size(max=120) private String name="";@Size(max=2000) private String description="";
 @Size(max=160) private String deliveryName="";@Size(max=500) private String deliveryDirections="";
 public Long getVersion(){return version;} public void setVersion(Long v){version=v;}
 public String getName(){return name;}public void setName(String v){name=v;}
 public String getDescription(){return description;}public void setDescription(String v){description=v;}
 public String getDeliveryName(){return deliveryName;}public void setDeliveryName(String v){deliveryName=v;}
 public String getDeliveryDirections(){return deliveryDirections;}public void setDeliveryDirections(String v){deliveryDirections=v;}
 public static EventSectionForm from(Event e){var f=new EventSectionForm();f.version=e.getVersion();f.name=e.getName();f.description=e.getDescription();f.deliveryName=e.getDeliveryName();f.deliveryDirections=e.getDeliveryDirections();return f;}
}
