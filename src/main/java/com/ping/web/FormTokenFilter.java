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

@Component @org.springframework.core.annotation.Order(-110) @Profile("!bootstrap")
public class FormTokenFilter extends OncePerRequestFilter {
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        if (!request.getMethod().equals("GET") && !request.getMethod().equals("HEAD") && !request.getMethod().equals("OPTIONS")) {
            boolean floorplanUpload = request.getRequestURI().matches("/events/[0-9]+/floorplan/image")
                    && request.getContentType() != null && request.getContentType().startsWith("multipart/form-data");
            // Rejection only, before multipart parsing can close the connection. No mutation bypasses CSRF.
            if (floorplanUpload && request.getContentLengthLong() > 9L * 1024 * 1024) {
                uploadTooLarge(request,response); return;
            }
            HttpSession session = request.getSession(false);
            String expected = session == null ? null : (String) session.getAttribute("pingCsrfToken");
            String actual;
            try { actual = request.getParameter("_csrf"); }
            catch (IllegalStateException error) {
                if (floorplanUpload) { uploadTooLarge(request,response); return; }
                throw error;
            }
            if (expected == null || actual == null || !MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8))) {
                request.getRequestDispatcher("/errors/form-expired").forward(request, response);
                return;
            }
        }
        chain.doFilter(request, response);
    }
    private void uploadTooLarge(HttpServletRequest request,HttpServletResponse response) throws ServletException,IOException {
        request.setAttribute("floorplanBackUrl",request.getRequestURI().replaceFirst("/image$",""));
        request.getRequestDispatcher("/errors/floorplan-size").forward(request,response);
    }
}
