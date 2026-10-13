package com.ping.participant;

import jakarta.validation.constraints.*;

public class AvailabilityForm {
    @NotNull(message = "Selecciona la disponibilidad.") private Boolean available;
    @NotNull(message = "Recarga la página antes de cambiar la disponibilidad.") @PositiveOrZero private Long version;
    public Boolean getAvailable() { return available; }
    public void setAvailable(Boolean available) { this.available = available; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
