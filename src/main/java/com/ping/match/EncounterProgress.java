package com.ping.match;

import java.util.*;
import com.ping.review.ActivityReviewService.Review;
import com.ping.review.ActivityReviewService.Status;

/**
 * DEC-066 / DEC-070. Presentation only: derives the single operational card of an ACTIVE encounter from its real state,
 * from the participant's own perspective. It never authorises anything; every action is still validated by the services.
 *
 * <p>Phases: {@code arrive} (a required arrival is missing), {@code verify} (a required personal-code verification is
 * missing), {@code activity} (every requirement of this encounter's policy is met; the same condition as
 * {@link MatchView#canConfirm()}) and {@code finishing} (both confirmed; the Saga is crediting). Content of a later phase is
 * not rendered before the server reaches it.</p>
 */
public record EncounterProgress(List<Step> steps, String phase, String sub, String title, String detail, boolean waiting,
                                String lead, String moreInstructions) {
    public record Step(String label, String state) { }

    public int current() { for (int i = 0; i < steps.size(); i++) if (steps.get(i).state().equals("current")) return i + 1; return steps.size(); }
    public String currentLabel() { return steps.get(current() - 1).label(); }
    public boolean is(String phase) { return this.phase.equals(phase); }

    /** Coarse phase from the raw state; shared with the live state so a client can tell a new step from a same-step change. */
    public static String phase(int policy, boolean completionPending, boolean arrived, boolean otherArrived, boolean verified, boolean otherVerified) {
        if (completionPending) return "finishing";
        if (policy >= 2 && (!arrived || !otherArrived)) return "arrive";
        if (policy >= 2 && (!verified || !otherVerified)) return "verify";
        return "activity";
    }

    public static EncounterProgress of(MatchView m, Review review) {
        String other = m.otherAlias();
        boolean policy2 = m.verificationPolicy() >= 2, staff = m.staffReview(), delivery = m.atDelivery();
        boolean sent = review != null && review.status() != Status.CHANGES, approved = review != null && review.status() == Status.APPROVED;
        var labels = new ArrayList<String>();
        if (policy2) { labels.add("Llegar"); labels.add("Verificar"); }
        labels.add("Actividad");
        if (staff) labels.add("Revisión");
        labels.add(delivery ? "Marcar hecha" : "Confirmar");
        int act = labels.indexOf("Actividad"), rev = labels.indexOf("Revisión"), conf = labels.size() - 1;
        String phase = phase(m.verificationPolicy(), m.completionPending(), m.arrived(), m.otherArrived(), m.verified(), m.otherVerified());
        int index; String sub, title, detail = null; boolean waiting = false;
        switch (phase) {
            case "finishing" -> {
                index = labels.size() - 1; sub = "retry"; title = "Finalizando tu encuentro"; waiting = true;
                detail = "Ambos confirmaron. Estamos acreditando los puntos una sola vez.";
            }
            case "arrive" -> {
                index = 0;
                if (!m.arrived()) { sub = "go"; title = "Llega al punto de encuentro"; detail = m.otherArrived() ? other + " ya llegó." : null; }
                else { sub = "wait"; title = "Esperando a " + other; detail = "Tu llegada está registrada."; waiting = true; }
            }
            case "verify" -> {
                index = 1;
                if (!m.verified() && !m.otherVerified()) { sub = "both"; title = "Verifica a " + other;
                    detail = "Escanea su QR o muéstrale el tuyo; pueden elegir quién empieza."; }
                else if (!m.verified()) { sub = "scan"; title = other + " ya te verificó. Escanea su QR";
                    detail = "Pídele su QR personal o escribe el código que te muestra."; }
                else { sub = "show"; title = "Muéstrale tu QR a " + other;
                    detail = "Tu verificación quedó registrada. Falta que " + other + " escanee tu QR."; }
            }
            default -> {
                // Activity: the card shows the activity itself; title/detail describe only its action zone.
                if (staff && review == null) { index = act; sub = "submit"; title = "Envíenla a revisión";
                    detail = "Basta con que una persona la envíe. Enviar no es aprobar ni suma puntos."; }
                else if (staff && !sent) { index = act; sub = "fix"; title = "Corrige y reenvía";
                    detail = "El personal pidió una corrección. Cualquiera de los dos puede reenviarla."; }
                else if (!m.confirmed()) {
                    index = staff ? (approved ? conf : rev) : act; sub = delivery ? "mark" : "confirm";
                    if (delivery) { title = "¿La hicieron? Márquenla como hecha";
                        detail = m.otherConfirmed() ? other + " ya la marcó como hecha. Falta tu parte." : "Los puntos llegan cuando ambos la marquen; la evidencia se comprueba al recoger el premio."; }
                    else if (staff) { title = approved ? "Aprobada · falta tu confirmación" : "En revisión · confirma tu parte";
                        detail = approved ? (m.otherConfirmed() ? other + " ya confirmó. Al confirmar tú, se acreditan los puntos." : "El personal la aprobó. Confirma que la hicieron.")
                                : "El personal aún no la aprueba. Puedes confirmar ya; los puntos llegan cuando la apruebe."; }
                    else { title = "¿La terminaron? Confírmalo";
                        detail = m.otherConfirmed() ? other + " ya confirmó. Falta tu confirmación." : "Cada persona confirma desde su celular cuando la hayan hecho."; }
                }
                else if (!m.otherConfirmed()) { index = conf; sub = "wait"; waiting = true; title = "Esperando a " + other;
                    detail = (delivery ? "Marcaste la actividad como hecha. Falta que " + other + " la marque."
                            : "Tu confirmación está guardada. Falta que " + other + " confirme" + (staff && !approved ? " y que el personal la apruebe." : ".")); }
                else if (staff && !approved) { index = rev; sub = "review"; waiting = true; title = "En revisión del personal";
                    detail = "Ambos confirmaron. Los puntos llegan cuando el personal la apruebe."; }
                else { index = conf; sub = "retry"; waiting = true; title = "Finalizando tu encuentro";
                    detail = "Ambos confirmaron. Si no termina, puedes reintentar sin duplicar puntos."; }
            }
        }
        var steps = new ArrayList<Step>();
        for (int i = 0; i < labels.size(); i++) steps.add(new Step(labels.get(i), i < index ? "done" : i == index ? "current" : "todo"));
        String lead = null, more = null;
        if (phase.equals("activity")) { String[] parts = split(m.activity()); lead = parts[0]; more = parts[1]; }
        return new EncounterProgress(List.copyOf(steps), phase, sub, title, detail, waiting, lead, more);
    }

    /** Long text: first sentence in view and the rest on request («Ver …»); short text stays whole. Never cut silently. */
    static String[] split(String text) {
        if (text == null || text.isBlank()) return new String[]{null, null};
        String t = text.strip();
        if (t.length() <= 200) return new String[]{t, null}; // short enough: every condition stays in view
        var matcher = java.util.regex.Pattern.compile("[.!?](\\s+)").matcher(t);
        if (matcher.find() && matcher.start() >= 12 && matcher.start() <= 180) {
            String rest = t.substring(matcher.end()).strip();
            return new String[]{t.substring(0, matcher.start() + 1), rest.isEmpty() ? null : rest};
        }
        return new String[]{t, null};
    }
}
