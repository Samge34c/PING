package com.ping.match;
import java.util.*;
public final class ActivityCatalog {
    private ActivityCatalog() { }
    private static final List<String> ORDER = List.of("Videojuegos", "Música", "Cine", "Deportes", "Tecnología", "Anime", "Viajes", "Fotografía");
    private static final List<String> WORDS = List.of("PIXEL", "RITMO", "ESCENA", "META", "NODO", "MANGA", "RUTA", "FOCO");
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
