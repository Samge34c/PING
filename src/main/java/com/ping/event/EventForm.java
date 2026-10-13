package com.ping.event;

import jakarta.validation.constraints.*;

public class EventForm {
    @NotBlank(message = "Escribe el nombre del evento.")
    @Size(max = 120, message = "El nombre puede tener hasta 120 caracteres.")
    private String name = "";
    @Size(max = 2000, message = "La descripción puede tener hasta 2000 caracteres.")
    private String description = "";
    @PositiveOrZero private Long version;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public static EventForm from(Event event) {
        EventForm form = new EventForm();
        form.name = event.getName(); form.description = event.getDescription(); form.version = event.getVersion();
        return form;
    }
}
