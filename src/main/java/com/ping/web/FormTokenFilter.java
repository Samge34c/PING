package com.ping.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component @Profile("!bootstrap")
public class FormTokenFilter extends OncePerRequestFilter {
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        if (!request.getMethod().equals("GET") && !request.getMethod().equals("HEAD") && !request.getMethod().equals("OPTIONS")) {
            HttpSession session = request.getSession(false);
            String expected = session == null ? null : (String) session.getAttribute("pingCsrfToken");
            String actual = request.getParameter("_csrf");
            if (expected == null || actual == null || !MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8))) {
                response.setStatus(403); response.setContentType("text/html;charset=UTF-8");
                response.getWriter().write("""
                        <!doctype html><html lang="es"><meta charset="UTF-8"><title>Formulario vencido · Ping</title>
                        <h1>Vuelve a abrir el formulario</h1><p>No se guardaron cambios. Recarga la página y vuelve a intentarlo.</p>
                        <a href="/">Volver a Ping</a></html>
                        """);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
