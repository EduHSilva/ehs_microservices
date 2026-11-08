package com.edu.silva.budgets.infra.config;

import java.util.UUID;

public class UserContext {
    private static final ThreadLocal<UUID> currentUser = new ThreadLocal<>();

    public static void setUser(UUID userId) {
        currentUser.set(userId);
    }

    public static UUID getUser() {
        return currentUser.get();
    }

    public static void clear() {
        currentUser.remove();
    }
}
