package com.ping.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;

final class ErrorRecovery {
    private ErrorRecovery() { }
    static void configure(HttpServletRequest request, Model model, boolean missing) {
        var session = request.getSession(false);
        boolean entered = session != null && session.getAttribute("pingDemoParticipantId") instanceof Long;
        String path = request.getRequestURI(), url = entered ? "/me" : "/join";
        String label = entered ? "Volver a Mi Ping" : "Entrar con el código del evento";
        if (entered && path.matches("/matches/[0-9]+(/[^/]+)?")) {
            url = missing ? "/connect" : path.replaceFirst("(/matches/[0-9]+).*", "$1");
            label = missing ? "Ir a Conectar" : "Revisar el encuentro";
        } else if (entered && (path.equals("/connect") || path.matches("/me/(search|availability)"))) {
            url = "/connect"; label = "Revisar Conectar";
        } else if (entered && path.equals("/me/benefits")) {
            url = "/me/benefits"; label = "Revisar mis canjes";
        } else if (!missing && path.matches("/events/[0-9]+/floorplan(/(image|position|remove))?")) {
            url = path.replaceFirst("(/events/[0-9]+/floorplan).*", "$1"); label = "Volver al plano";
        }
        model.addAttribute("recoveryUrl", url); model.addAttribute("recoveryLabel", label);
    }
}
