package com.ping.match;
import java.util.*;
public final class ActivityCatalog {
    private ActivityCatalog() { }
    private static final List<String> ORDER = List.of("Videojuegos", "Música", "Cine", "Deportes", "Tecnología", "Anime", "Viajes", "Fotografía");
    private static final List<String> WORDS = List.of("PIXEL", "RITMO", "ESCENA", "META", "NODO", "MANGA", "RUTA", "FOCO");
    public record Encounter(String meetingPoint, String word, String activity) { }
    private record SofaActivity(String point, String word, String activity) { }
    private static final SofaActivity NEUTRAL = new SofaActivity("Meet & Greet · Pabellón 23", "PING",
        "Cada uno cuente cuál ha sido su zona favorita del SOFA y elijan una para recorrer juntos.");
    private static final Map<String, SofaActivity> SOFA = Map.of(
        "Videojuegos", new SofaActivity("Zona Gamer · Pabellón 18", "PIXEL",
            "Elijan un juego o stand de la Zona Gamer que ambos probarían y cuenten qué les llamó la atención."),
        "Tecnología", new SofaActivity("Stage Cultura Gamer · Pabellón 17", "CODIGO",
            "Encuentren un proyecto o idea tecnológica del SOFA que les parezca interesante y comparen qué mejorarían."),
        "Anime", new SofaActivity("Comunidades SOFA · Pabellón 3 Nivel 2", "MANGA",
            "Encuentren un personaje que ambos reconozcan y recomiéndense una serie."),
        "Fotografía", new SofaActivity("Cosplay Parade · Pabellón 8 Nivel 2", "FLASH",
            "Busquen un rincón visual del SOFA y elijan juntos qué imagen representa mejor el evento."),
        "Música", new SofaActivity("SOFA Stage · Pabellón 17", "RITMO",
            "Recomiéndense un opening, soundtrack o canción geek y elijan una para escuchar después."),
        "Cine", new SofaActivity("Meet & Greet · Pabellón 23", "ESCENA",
            "Elijan una película o serie que ambos convertirían en cosplay y cuenten qué personaje escogerían.")
    );
    public static String meetingPoint(String interests) { return sofaActivity(interests).point(); }
    private static SofaActivity sofaActivity(String interests) { return SOFA.getOrDefault(topic(interests), NEUTRAL); }
    public static Encounter encounter(String interests, long matchId, String eventName, String defaultPoint) {
        // SOFA is an academic demo context; other events keep their configured meeting point and catalog.
        if (!"SOFA 2026".equals(eventName)) return new Encounter(defaultPoint, word(interests, matchId), activity(interests));
        SofaActivity selected = sofaActivity(interests);
        String word = selected.word() + "-" + Long.toString(matchId, 36).toUpperCase(Locale.ROOT);
        return new Encounter(meetingPoint(interests), word, selected.activity());
    }
    public static Encounter encounter(String interests, long matchId, com.ping.event.Event event) {
        Encounter fallback = encounter(interests, matchId, event.getName(), event.getMeetingPoint());
        String base = com.ping.event.EncounterOptions.select(event.getEncounterWords(), matchId, null);
        return new Encounter(com.ping.event.EncounterOptions.select(event.getMeetingPoints(), matchId, fallback.meetingPoint()),
            base == null ? fallback.word() : base + "-" + Long.toString(matchId, 36).toUpperCase(Locale.ROOT),
            com.ping.event.EncounterOptions.select(event.getEncounterActivities(), matchId, fallback.activity()));
    }
    public static String topic(String interests) { return ORDER.stream().filter(s -> Arrays.asList(interests.split(" · ")).contains(s)).findFirst().orElse(""); }
    public static String word(String interests, long matchId) {
        int index = ORDER.indexOf(topic(interests));
        return (index < 0 ? "PING" : WORDS.get(index)) + "-" + Long.toString(matchId, 36).toUpperCase(Locale.ROOT);
    }
    public static String activity(String interests) {
        return switch(topic(interests)) {
            case "Videojuegos" -> "Descubran un videojuego que ambos conozcan.";
            case "Música" -> "Cuéntense una canción que recomendarían.";
            case "Cine" -> "Encuentren una película que ambos hayan visto.";
            case "Tecnología" -> "Hablen de una tecnología que les parezca interesante.";
            case "Deportes" -> "Cuéntense un deporte que les gustaría practicar.";
            case "Anime" -> "Recomiéndense un anime y expliquen por qué.";
            case "Viajes" -> "Compartan un lugar que les gustaría visitar.";
            case "Fotografía" -> "Imaginen una foto que represente este evento.";
            default -> "Encuentren algo en lo que estén de acuerdo y una cosa en la que sean diferentes.";
        };
    }
}
