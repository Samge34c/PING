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
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class) public String concurrentChange(Model model, HttpServletResponse response) {
        response.setStatus(409); model.addAttribute("errorMessage", "Los datos cambiaron. Recarga la página e inténtalo otra vez."); return "errors/message";
    }
}
