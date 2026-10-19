package com.ping.web;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
@Controller @Profile("!bootstrap")
public class ErrorPageController {
    @RequestMapping("/errors/floorplan-size") public String uploadSize(jakarta.servlet.http.HttpServletRequest request,Model model,HttpServletResponse response) {
        response.setStatus(413);Object back=request.getAttribute("floorplanBackUrl");
        model.addAttribute("backUrl",back instanceof String path && path.matches("/events/[0-9]+/floorplan") ? path : "/events");
        return "floorplan/upload-error";
    }
    // La ruta únicamente presenta el error; no procesa el formulario original.
    @RequestMapping("/errors/form-expired") public String expired(jakarta.servlet.http.HttpServletRequest request, Model model, HttpServletResponse response) {
        ErrorRecovery.configure(request, model, false);
        response.setStatus(403); model.addAttribute("errorTitle", "Vuelve a abrir el formulario");
        var session = request.getSession(false);
        boolean entered = session != null && session.getAttribute("pingDemoParticipantId") instanceof Long;
        model.addAttribute("errorMessage", entered ? "Este formulario caducó o ya no es válido. No se guardaron cambios. Revisa el estado antes de repetir la acción." : "El formulario o la sesión ya no son válidos. No se guardaron cambios. Vuelve a entrar; si perdiste la sesión, no podemos recuperar aquí tu identidad ni tu saldo anterior.");
        return "errors/message";
    }
}
