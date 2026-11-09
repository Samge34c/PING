package com.ping.auth;

import java.util.*;
import com.ping.encounter.*;
import com.ping.event.*;
import com.ping.reward.*;
import com.ping.review.ActivityReviewService;

/**
 * DEC-066. Organisation board for ONE selected event (Figma 189:340). Read-only summary of real configuration;
 * it adds no opening requirement: «required» only repeats what activation already needs for new encounters.
 */
public record OrganizerBoard(Event event, long pendingReviews, long pendingClaims, List<Row> prepare, boolean ready) {
    public enum Tone { OK, TODO, OPTIONAL }
    public record Row(String title, String state, Tone tone, String action, String href, boolean required) { }

    static OrganizerBoard of(Event e, EncounterService encounters, RewardService rewards, ActivityReviewService reviews, String entryLink) {
        long id = e.getId(); String base = "/events/" + id;
        var rows = new ArrayList<Row>();
        rows.add(new Row("Datos del evento", e.getDescription() == null || e.getDescription().isBlank() ? "Nombre listo · sin descripción (opcional)" : "Nombre y descripción listos", Tone.OK, "Editar", base + "/encounters#essentials", false));
        boolean structured = e.isStructuredEncounter();
        var points = structured ? encounters.points(id).stream().filter(p -> p.isActive() && !p.isArchived()).toList() : List.<MeetingPoint>of();
        var tasks = structured ? encounters.activities(id).stream().filter(a -> a.isActive() && !a.isArchived()).toList() : List.<EventActivity>of();
        long compatible = tasks.stream().filter(a -> points.stream().anyMatch(p -> a.getPointIds().contains(p.getId()))).count();
        if (structured) {
            rows.add(new Row("Lugares de encuentro", points.isEmpty() ? "Falta un lugar activo para nuevos encuentros" : points.size() + (points.size() == 1 ? " lugar activo: " : " lugares activos: ") + String.join(" · ", points.stream().map(MeetingPoint::getName).toList()),
                    points.isEmpty() ? Tone.TODO : Tone.OK, points.isEmpty() ? "Añadir lugar" : "Editar", base + "/encounters#points", true));
            String range = tasks.isEmpty() ? "" : " · " + (tasks.stream().mapToInt(EventActivity::getRewardPoints).min().getAsInt() == tasks.stream().mapToInt(EventActivity::getRewardPoints).max().getAsInt()
                    ? "+" + tasks.getFirst().getRewardPoints() + " puntos cada una" : "de +" + tasks.stream().mapToInt(EventActivity::getRewardPoints).min().getAsInt() + " a +" + tasks.stream().mapToInt(EventActivity::getRewardPoints).max().getAsInt() + " puntos");
            long reviewed = tasks.stream().filter(a -> a.getVerificationMethod() == CheckMethod.STAFF_REVIEW).count();
            rows.add(new Row("Actividades y puntos", compatible == 0 ? "Falta una actividad activa con un lugar activo" : tasks.size() + (tasks.size() == 1 ? " activa" : " activas") + range + (reviewed > 0 ? " · " + reviewed + " con revisión del personal" : ""),
                    compatible == 0 ? Tone.TODO : Tone.OK, compatible == 0 ? "Añadir actividad" : "Editar", base + "/encounters#activities", true));
        } else {
            rows.add(new Row("Lugares de encuentro", "Lista anterior · " + (EncounterOptions.unique(e.getMeetingPoints()).isEmpty() ? e.getMeetingPoint() : String.join(" · ", EncounterOptions.unique(e.getMeetingPoints()))), Tone.OK, "Editar", base + "/encounters#points", true));
            rows.add(new Row("Actividades y puntos", "Lista anterior · +" + EncounterService.LEGACY_REWARD_POINTS + " puntos cada una", Tone.OK, "Editar", base + "/encounters#activities", true));
        }
        rows.add(new Row("Plano del evento", e.getFloorplanId() == null ? "Opcional · sin plano; se usan las indicaciones" : "Plano cargado", e.getFloorplanId() == null ? Tone.OPTIONAL : Tone.OK, e.getFloorplanId() == null ? "Cargar plano" : "Ubicar lugares", base + "/floorplan", false));
        var benefits = rewards.catalog(id).stream().filter(b -> b.getEventId() != null).toList();
        boolean deliveryMissing = benefits.stream().anyMatch(Benefit::isDefaultDelivery) && (e.getDeliveryName().isBlank() || e.getDeliveryDirections().isBlank());
        long withImage = benefits.stream().filter(Benefit::hasImage).count();
        rows.add(new Row("Recompensas", benefits.isEmpty() ? "Opcional · ninguna; los puntos aún no se pueden canjear" : deliveryMissing ? "Falta el lugar de entrega predeterminado" : benefits.size() + (benefits.size() == 1 ? " disponible" : " disponibles") + " · " + withImage + " con imagen",
                benefits.isEmpty() ? Tone.OPTIONAL : deliveryMissing ? Tone.TODO : Tone.OK, benefits.isEmpty() ? "Crear recompensa" : "Editar", base + "/rewards", false));
        rows.add(new Row("Ayuda, agenda y reto", "Opcional · contacto del personal y agenda general", Tone.OPTIONAL, "Revisar", base + "/preparation", false));
        boolean ready = rows.stream().noneMatch(r -> r.required() && r.tone() == Tone.TODO);
        return new OrganizerBoard(e, reviews.open(id), rewards.forEvent(id).requested(), List.copyOf(rows), ready);
    }
}
