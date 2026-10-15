package com.ping.experience;

import java.util.List;
import com.ping.match.ActivityCatalog;
import com.ping.match.MatchStatus;
import com.ping.match.MatchView;

/** Local presentation fixtures. No rewards, tracking or changes to match state. */
public final class EventExperienceCatalog {
    private EventExperienceCatalog() { }
    public record Zone(String id, String name, String point, String interest, String icon, String description,
                       String reason, String pavilion, int x, int y) { }
    public record Experience(String name, String location, String dates, List<Zone> zones) {
        public List<Zone> markers() {
            var seen = new java.util.HashSet<String>();
            return zones.stream().filter(z -> seen.add(z.pavilion())).toList();
        }
        public Zone zone(String id) { return zones.stream().filter(z -> z.id().equals(id)).findFirst().orElse(null); }
        public Zone atPoint(String point) { return zones.stream().filter(z -> z.point().equals(point)).findFirst().orElse(null); }
    }
    private static final Experience SOFA = new Experience("SOFA 2026", "Corferias · Bogotá", "8–12 OCT 2026", List.of(
        new Zone("gamer", "Zona Gamer", ActivityCatalog.meetingPoint("Videojuegos"), "Videojuegos", "game",
            "Open Games y desarrolladores, en el pabellón 18.", "Un juego puede ser el inicio de una buena conversación.", "18", 64, 22),
        new Zone("technology", "Stage Cultura Gamer", ActivityCatalog.meetingPoint("Tecnología"), "Tecnología", "bolt",
            "Escenario Cultura Gamer del pabellón 17.", "Comparen ideas y proyectos que les llamen la atención.", "17", 45, 22),
        new Zone("communities", "Comunidades SOFA", ActivityCatalog.meetingPoint("Anime"), "Anime", "users",
            "Comunidades y aficiones, en el pabellón 3, nivel 2.", "Encuentra historias y personajes que ambos reconozcan.", "3", 28, 52),
        new Zone("cosplay", "Cosplay Parade", ActivityCatalog.meetingPoint("Fotografía"), "Fotografía", "camera",
            "Estilos alternativos, en el pabellón 8, nivel 2.", "Un rincón visual para conversar sobre lo que hace único al evento.", "8", 82, 68),
        new Zone("stage", "SOFA Stage", ActivityCatalog.meetingPoint("Música"), "Música", "music",
            "Escenario de videojuegos del pabellón 17; punto de encuentro demo para Música.", "Compartan un opening, soundtrack o canción geek.", "17", 45, 22),
        new Zone("meet", "Meet & Greet", ActivityCatalog.meetingPoint("Cine"), "Cine", "film",
            "Meet & Greet en el pabellón 23, según la programación de SOFA.", "Un punto neutral para descubrir otros intereses.", "23", 84, 22)
    ));
    public static Experience forEvent(String name) { return SOFA.name().equals(name) ? SOFA : null; }
    public static List<String> badges(List<MatchView> history) {
        var completed = history.stream().filter(m -> m.status() == MatchStatus.COMPLETED).toList();
        var result = new java.util.ArrayList<String>();
        if (!completed.isEmpty()) result.add("Primer Ping");
        if (completed.size() >= 2) result.add("Conector");
        if (completed.stream().anyMatch(m -> m.commonInterests().isBlank())) result.add("Explorador");
        if (!completed.isEmpty()) result.add("SOFA Link");
        return List.copyOf(result);
    }
    public static boolean promoUnlocked(List<MatchView> history) { return history.stream().anyMatch(m -> m.status() == MatchStatus.COMPLETED); }
}
