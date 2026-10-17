package com.ping.event;

import java.util.List;
import com.ping.web.BusinessRuleException;

/** Small, event-local catalogs; no generated content or remote lookup. */
public final class EncounterOptions {
    private EncounterOptions() { }
    public static List<String> lines(String value) {
        return value == null ? List.of() : value.lines().map(String::strip).filter(s -> !s.isEmpty()).toList();
    }
    public static String normalize(String value, int maxLength, String label) {
        var lines = lines(value);
        if (lines.size() > 20 || lines.stream().anyMatch(s -> s.length() > maxLength || s.codePoints().anyMatch(Character::isISOControl))) {
            throw new BusinessRuleException(label + ": usa hasta 20 opciones, de máximo " + maxLength + " caracteres cada una, sin caracteres de control.");
        }
        return String.join("\n", lines.stream().distinct().toList());
    }
    public static String select(String options, long matchId, String fallback) {
        var lines = lines(options);
        return lines.isEmpty() ? fallback : lines.get((int) Math.floorMod(matchId - 1, (long) lines.size()));
    }
}
