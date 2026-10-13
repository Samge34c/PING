package com.ping.interest;

import java.util.ArrayList;
import java.util.List;
import jakarta.validation.constraints.*;

public class InterestSelectionForm {
    @NotNull(message = "Selecciona intereses válidos.")
    private List<@NotNull @Positive Long> interestIds = new ArrayList<>();
    @NotNull(message = "Recarga la página antes de guardar.") @PositiveOrZero private Long version;
    public List<Long> getInterestIds() { return interestIds; }
    public void setInterestIds(List<Long> interestIds) { this.interestIds = interestIds; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
