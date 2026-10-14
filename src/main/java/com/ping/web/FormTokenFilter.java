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
                request.getRequestDispatcher("/errors/form-expired").forward(request, response);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
