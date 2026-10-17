package com.edu.silva.tasks.infra.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
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
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull  FilterChain filterChain
    ) throws ServletException, IOException {

        String userIdHeader = request.getHeader("X-User-ID");

        if (userIdHeader != null) {
            try {
                UUID userId = UUID.fromString(userIdHeader);
                UserContext.setUser(userId);

                Session session = entityManager.unwrap(Session.class);
                session.enableFilter("userFilter")
                        .setParameter("userId", userId);
            } catch (Exception e) {
                throw new ServletException(e);
            }

            try {
                filterChain.doFilter(request, response);
            } finally {
                UserContext.clear();
            }
            return;
        }

        filterChain.doFilter(request, response);
    }
}
