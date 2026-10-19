package com.ping.event;

import jakarta.validation.constraints.*;

public class EventForm {
    @NotBlank(message = "Escribe el nombre del evento.")
    @Size(max = 120, message = "El nombre puede tener hasta 120 caracteres.")
    private String name = "";
    @Size(max = 2000, message = "La descripción puede tener hasta 2000 caracteres.")
    private String description = "";
    @Size(max = 160, message = "El punto puede tener hasta 160 caracteres.")
    private String meetingPoint = "Punto Ping del evento";
    public String getMeetingPoint() { return meetingPoint; }
    public void setMeetingPoint(String value) { meetingPoint = value; }
    @Size(max = 4000) private String meetingPoints = "";
    @Size(max = 5000) private String encounterWords = "";
    @Size(max = 12000) private String encounterActivities = "";
    public String getMeetingPoints() { return meetingPoints; }
    public void setMeetingPoints(String value) { meetingPoints = value; }
    public String getEncounterWords() { return encounterWords; }
    public void setEncounterWords(String value) { encounterWords = value; }
    public String getEncounterActivities() { return encounterActivities; }
    public void setEncounterActivities(String value) { encounterActivities = value; }
    @AssertTrue(message = "Revisa los límites:3 puntos,100 palabras únicas y10 actividades, sin caracteres de control.")
    public boolean isEncounterOptionsValid() {
        try {
            EncounterOptions.normalize(meetingPoints, 160, "Puntos");
            EncounterOptions.normalize(encounterWords, 40, "Palabras");
            EncounterOptions.normalize(encounterActivities, 500, "Actividades");
            return true;
        } catch (com.ping.web.BusinessRuleException error) { return false; }
    }
    @Size(max=160) private String deliveryName="";
    @Size(max=500) private String deliveryDirections="";
    public String getDeliveryName(){return deliveryName;} public void setDeliveryName(String v){deliveryName=v==null?"":v;}
    public String getDeliveryDirections(){return deliveryDirections;} public void setDeliveryDirections(String v){deliveryDirections=v==null?"":v;}
    @PositiveOrZero private Long version;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public static EventForm from(Event event) {
        EventForm form = new EventForm();
        form.deliveryName=event.getDeliveryName();form.deliveryDirections=event.getDeliveryDirections();
        form.meetingPoints = event.getMeetingPoints(); form.encounterWords = event.getEncounterWords(); form.encounterActivities = event.getEncounterActivities();
        form.meetingPoint = event.getMeetingPoint(); form.name = event.getName(); form.description = event.getDescription(); form.version = event.getVersion();
        return form;
    }
}
