package com.ping.participant;

import java.util.List;
import jakarta.validation.constraints.*;

/** Private form; deliberately not part of ParticipantView or public listings. */
public class RecognitionHintForm {
    private String hint = "";
    @NotNull(message = "Recarga el formulario antes de guardar.") @PositiveOrZero private Long version;
    public String getHint() { return hint; }
    public void setHint(String hint) { this.hint = hint; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    // @Size counts Unicode code points, consistently with MySQL VARCHAR(120) and the JS counter.
    @Size(max = 120, message = "Usa como máximo 120 caracteres.")
    public List<Integer> getHintCharacters() { return hint == null ? List.of() : hint.codePoints().limit(121).boxed().toList(); }
}
