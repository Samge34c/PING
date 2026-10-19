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
    @ExceptionHandler(SessionRequiredException.class)
    public Object sessionRequired(SessionRequiredException error, HttpServletRequest request, Model model, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, private");
        if (request.getRequestURI().equals("/me/state") || request.getRequestURI().matches("/matches/[0-9]+/recognition-hint")) {
            return org.springframework.http.ResponseEntity.status(401).body(java.util.Map.of("error", "SESSION_REQUIRED", "message", error.getMessage(), "entryUrl", "/join"));
        }
        response.setStatus(401); model.addAttribute("errorTitle", "Vuelve a entrar a tu evento");
        model.addAttribute("errorMessage", error.getMessage()); return errorView(request, model);
    }
    private String errorView(HttpServletRequest request, Model model) {
        if (!model.containsAttribute("recoveryUrl")) ErrorRecovery.configure(request, model, false);
        return "errors/message";
    }
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public String uploadLimit(HttpServletRequest request,Model model,HttpServletResponse response) {
        response.setStatus(413);
        Object saved=request.getAttribute("floorplanBackUrl");
        String back=saved instanceof String path ? path : request.getRequestURI().replaceFirst("/image$","");
        model.addAttribute("backUrl",back.matches("/events/[0-9]+/floorplan") ? back : "/events");
        return "floorplan/upload-error";
    }
    @ModelAttribute public void privatePageCache(HttpServletRequest request, HttpServletResponse response) {
        var session = request.getSession(false);
        if ((session != null && session.getAttribute("pingDemoParticipantId") != null) || request.getRequestURI().startsWith("/me") || request.getRequestURI().equals("/connect") || request.getRequestURI().startsWith("/matches") || request.getRequestURI().startsWith("/join")) {
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
    public String invalidInput(Exception error, HttpServletRequest request, Model model, HttpServletResponse response) {
        response.setStatus(400); model.addAttribute("errorMessage", "Revisa los datos y abre de nuevo el formulario. No se guardaron cambios."); return errorView(request, model);
    }
    @ExceptionHandler(NotFoundException.class) public String notFound(NotFoundException error, HttpServletRequest request, Model model, HttpServletResponse response) {
        ErrorRecovery.configure(request, model, true);
        response.setStatus(404); model.addAttribute("errorTitle", "No encontramos este recurso"); model.addAttribute("errorMessage", error.getMessage()); return errorView(request, model);
    }
    @ExceptionHandler(BusinessRuleException.class) public String conflict(BusinessRuleException error, HttpServletRequest request, Model model, HttpServletResponse response) {
        response.setStatus(409); model.addAttribute("errorMessage", error.getMessage()); return errorView(request, model);
    }
    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    public String storageUnavailable(HttpServletRequest request, Model model, HttpServletResponse response) {
        response.setStatus(503); model.addAttribute("errorMessage", "No pudimos completar la operación con los datos guardados. Recarga y comprueba su estado antes de reintentar."); return errorView(request, model);
    }
    @ExceptionHandler(Exception.class)
    public String unexpected(Exception error, HttpServletRequest request, Model model, HttpServletResponse response) {
        if (error instanceof org.springframework.web.ErrorResponse webError) {
            response.setStatus(webError.getStatusCode().value());
            ErrorRecovery.configure(request, model, true);
            model.addAttribute("errorTitle", webError.getStatusCode().value() == 404 ? "No encontramos esta página" : "Esta acción no está disponible");
            model.addAttribute("errorMessage", "El enlace puede haber cambiado o ya no estar disponible. Continúa desde PING."); return errorView(request, model);
        }
        String reference = UUID.randomUUID().toString().substring(0, 8);
        org.slf4j.LoggerFactory.getLogger(WebAdvice.class).error("Error de Ping, referencia {}", reference, error);
        response.setStatus(500); model.addAttribute("errorMessage", "No pudimos completar esta acción. Comprueba el estado antes de repetirla. Referencia: " + reference); return errorView(request, model);
    }
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class) public String concurrentChange(HttpServletRequest request, Model model, HttpServletResponse response) {
        response.setStatus(409); model.addAttribute("errorMessage", "Los datos cambiaron. Recarga la página e inténtalo otra vez."); return errorView(request, model);
    }
}
