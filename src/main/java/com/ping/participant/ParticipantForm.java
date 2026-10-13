package com.ping.participant;

import jakarta.validation.constraints.*;

public class ParticipantForm {
    @NotBlank(message = "Escribe un alias.") @Size(max = 80, message = "El alias puede tener hasta 80 caracteres.")
    private String alias = "";
    @NotNull(message = "Selecciona un evento.") @Positive(message = "Selecciona un evento válido.") private Long eventId;
    private boolean available;
    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }
    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
}
