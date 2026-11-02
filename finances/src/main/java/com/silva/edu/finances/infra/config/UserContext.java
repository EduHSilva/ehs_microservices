package com.silva.edu.finances.infra.config;

import java.util.UUID;

public final class UserContext {
    private static final ThreadLocal<UUID> CURRENT_USER = new ThreadLocal<>();

    private UserContext() {}

    public static void setUser(UUID userId) {
        CURRENT_USER.set(userId);
    }

    public static UUID getUser() {
        UUID userId = CURRENT_USER.get();
        if (userId == null) {
            throw new IllegalStateException("Authenticated user is not available");
        }
        return userId;
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
