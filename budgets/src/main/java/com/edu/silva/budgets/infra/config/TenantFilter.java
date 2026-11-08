package com.edu.silva.budgets.infra.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TenantFilter {

    @PersistenceContext
    private EntityManager entityManager;

    public UUID currentUserId() {
        UUID userId = UserContext.getUser();
        if (userId == null) {
            throw new IllegalStateException("User context is missing");
        }
        return userId;
    }

    public void apply() {
        UUID userId = currentUserId();
        entityManager.unwrap(Session.class)
                .enableFilter("userFilter")
                .setParameter("userId", userId);
    }
}
