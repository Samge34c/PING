package com.ping.match;

import java.util.*;
import com.ping.review.ActivityReviewService.Review;
import com.ping.review.ActivityReviewService.Status;

/**
 * DEC-066. Presentation only: derives «what now» for an ACTIVE encounter from its real state.
 * It never authorises anything; every action is still validated by the existing services.
 */
public record EncounterProgress(List<Step> steps, String title, String detail, String action, String target, boolean arriveForm, boolean waiting, boolean doneForm) {
    public record Step(String label, String state) { }

    public int current() { for (int i = 0; i < steps.size(); i++) if (steps.get(i).state().equals("current")) return i + 1; return steps.size(); }

    public static EncounterProgress of(MatchView m, Review review) {
        String other = m.otherAlias();
        boolean policy2 = m.verificationPolicy() >= 2, staff = m.staffReview(), delivery = m.atDelivery();
        boolean arrivals = m.arrived() && m.otherArrived(), codes = m.verified() && m.otherVerified();
        boolean sent = review != null && review.status() != Status.CHANGES, approved = review != null && review.status() == Status.APPROVED;
        var labels = new ArrayList<String>();
        if (policy2) { labels.add("Llegar"); labels.add("Verificar"); }
        labels.add("Actividad");
        if (staff) labels.add("Revisión");
        labels.add(delivery ? "Marcar hecha" : "Confirmar");
        int index; String title, detail, action = null, target = null; boolean arrive = false, waiting = false;
        int act = labels.indexOf("Actividad"), rev = labels.indexOf("Revisión"), conf = labels.size() - 1;
        boolean done = false;
        if (m.completionPending()) {
            index = labels.size(); title = "Finalizando tu encuentro";
            detail = "Ambos confirmaron. Estamos acreditando los puntos una sola vez.";
            action = "Ver estado"; target = "#am-confirm-title"; waiting = true;
        } else if (policy2 && !m.arrived()) {
            index = 0; title = "Llega a " + Objects.toString(m.meetingPoint(), "tu punto de encuentro");
            detail = "Cuando estés allí, regístralo. Llegar no da puntos: abre la verificación.";
            action = "Ya llegué"; arrive = true;
        } else if (policy2 && !m.otherArrived()) {
            index = 0; title = "Esperando a " + other;
            detail = "Tu llegada está guardada. Cuando " + other + " registre la suya, verán aquí el siguiente paso.";
            waiting = true; target = "#am-location-title"; action = "Ver el punto";
        } else if (policy2 && !m.verified()) {
            index = 1; title = "Verifica el código de " + other;
            detail = m.otherVerified() ? other + " ya verificó tu código. Falta tu parte." : "Pídele su código personal o su QR y escríbelo o escanéalo.";
            action = "Verificar código"; target = "#am-verify-title";
        } else if (policy2 && !m.otherVerified()) {
            index = 1; title = "Muestra tu código a " + other;
            detail = "Tu verificación está guardada. Falta que " + other + " verifique el tuyo.";
            action = "Mostrar mi código"; target = "#am-verify-title";
        } else if (staff && !sent) {
            index = act; title = review == null ? "Hagan la actividad y envíenla" : "Corrijan y vuelvan a enviar";
            detail = review == null ? "El personal del evento revisa lo que envíen. Basta con que una persona lo envíe."
                    : "El personal pidió una corrección: " + review.comment();
            action = review == null ? "Ver actividad y enviar" : "Ver la corrección"; target = "#am-review-title";
        } else if (delivery && !m.confirmed()) {
            // DEC-068: the existing bilateral confirmation is the «mark as done»; no second, equivalent confirmation.
            index = act; title = "¿Hicieron la actividad? Márquenla como hecha";
            detail = (m.otherConfirmed() ? other + " ya la marcó como hecha. " : "") + "Cuando ambos la marquen, cada persona recibe +" + m.rewardPoints() + " puntos una sola vez. Guarden la evidencia: la mostrarán al recoger el premio.";
            action = "Marcar actividad como hecha"; done = true;
        } else if (delivery && !m.otherConfirmed()) {
            index = conf; title = "Esperando a tu compañero";
            detail = "Marcaste la actividad como hecha. Falta que " + other + " la marque; los puntos llegan a los dos a la vez.";
            waiting = true;
        } else if (!m.confirmed()) {
            index = staff ? (approved ? conf : rev) : act;
            title = staff && !approved ? "Confirma que hicieron la actividad" : "¿Terminaron la actividad? Confírmalo";
            detail = staff && !approved ? "Ya la enviaron a revisión. Confirma tu parte; los puntos llegan cuando el personal la apruebe."
                    : "Cuando ambos confirmen, cada persona recibe +" + m.rewardPoints() + " puntos una sola vez.";
            action = "Confirmar actividad"; target = "#am-confirm-title";
        } else if (!m.otherConfirmed()) {
            index = conf; title = "Esperando a " + other;
            detail = "Tu confirmación está guardada. Falta que " + other + " confirme" + (staff && !approved ? " y que el personal apruebe la actividad." : ".");
            waiting = true;
        } else {
            index = rev < 0 ? conf : rev; title = "Esperando la revisión del personal";
            detail = "Ambos confirmaron. Recibirán +" + m.rewardPoints() + " puntos cada uno cuando el personal apruebe la actividad.";
            waiting = true; target = "#am-review-title"; action = "Ver lo enviado";
        }
        var steps = new ArrayList<Step>();
        for (int i = 0; i < labels.size(); i++) steps.add(new Step(labels.get(i), i < index ? "done" : i == index ? "current" : "todo"));
        return new EncounterProgress(List.copyOf(steps), title, detail, action, target, arrive, waiting, done);
    }
}
