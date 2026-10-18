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
    @RequestMapping("/errors/form-expired") public String expired(Model model, HttpServletResponse response) {
        response.setStatus(403); model.addAttribute("errorTitle", "Vuelve a abrir el formulario");
        model.addAttribute("errorMessage", "No se guardaron cambios. Abre de nuevo la página y vuelve a intentarlo."); return "errors/message";
    }
}
