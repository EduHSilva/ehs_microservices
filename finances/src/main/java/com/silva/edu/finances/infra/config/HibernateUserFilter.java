package com.silva.edu.finances.infra.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class HibernateUserFilter extends OncePerRequestFilter {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator/") || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs") || path.startsWith("/ping");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader("X-User-ID");
        UUID userId;
        try {
            userId = header == null ? null : UUID.fromString(header);
        } catch (IllegalArgumentException ex) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid X-User-ID");
            return;
        }

        if (userId == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "X-User-ID is required");
            return;
        }

        try {
            UserContext.setUser(userId);
            entityManager.unwrap(Session.class)
                    .enableFilter("userFilter")
                    .setParameter("userId", userId);
            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }
}
