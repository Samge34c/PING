package com.ping.web;

import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@ControllerAdvice @Profile("!bootstrap")
public class WebAdvice {
    @ModelAttribute public void privatePageCache(HttpServletRequest request, HttpServletResponse response) {
        if (request.getRequestURI().startsWith("/me") || request.getRequestURI().startsWith("/matches")) {
            response.setHeader("Cache-Control", "no-store, private"); response.setHeader("Expires", "0");
        }
    }
    @ModelAttribute("brandLogoAvailable") public boolean brandLogoAvailable() {
        return new org.springframework.core.io.ClassPathResource("static/img/ping-logo.jpg").exists();
    }
    @ModelAttribute("csrfToken") public String csrfToken(HttpServletRequest request) {
        var session = request.getSession();
        synchronized (session) {
            String token = (String) session.getAttribute("pingCsrfToken");
            if (token == null) { token = UUID.randomUUID().toString(); session.setAttribute("pingCsrfToken", token); }
            return token;
        }
    }
    @ExceptionHandler({org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class,
            org.springframework.validation.BindException.class, jakarta.validation.ConstraintViolationException.class})
    public String invalidInput(Exception error, Model model, HttpServletResponse response) {
        response.setStatus(400); model.addAttribute("errorMessage", "Revisa los datos del formulario y vuelve a intentarlo desde Mi Ping. No se guardaron cambios."); return "errors/message";
    }
    @ExceptionHandler(NotFoundException.class) public String notFound(NotFoundException error, Model model, HttpServletResponse response) {
        response.setStatus(404); model.addAttribute("errorMessage", error.getMessage()); return "errors/message";
    }
    @ExceptionHandler(BusinessRuleException.class) public String conflict(BusinessRuleException error, Model model, HttpServletResponse response) {
        response.setStatus(409); model.addAttribute("errorMessage", error.getMessage()); return "errors/message";
    }
    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    public String storageUnavailable(Model model, HttpServletResponse response) {
        response.setStatus(503); model.addAttribute("errorMessage", "No pudimos completar la operación con los datos guardados. Recarga y comprueba su estado antes de reintentar."); return "errors/message";
    }
    @ExceptionHandler(Exception.class)
    public String unexpected(Exception error, Model model, HttpServletResponse response) {
        if (error instanceof org.springframework.web.ErrorResponse webError) {
            response.setStatus(webError.getStatusCode().value());
            model.addAttribute("errorMessage", "Esta página o acción no está disponible. Vuelve a Ping y usa los enlaces y formularios de la aplicación."); return "errors/message";
        }
        String reference = UUID.randomUUID().toString().substring(0, 8);
        org.slf4j.LoggerFactory.getLogger(WebAdvice.class).error("Error de Ping, referencia {}", reference, error);
        response.setStatus(500); model.addAttribute("errorMessage", "No pudimos completar esta acción. Vuelve a Mi Ping y comprueba el estado. Referencia: " + reference); return "errors/message";
    }
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class) public String concurrentChange(Model model, HttpServletResponse response) {
        response.setStatus(409); model.addAttribute("errorMessage", "Los datos cambiaron. Recarga la página e inténtalo otra vez."); return "errors/message";
    }
}
