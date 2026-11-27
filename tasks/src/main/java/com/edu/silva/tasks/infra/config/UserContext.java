package com.edu.silva.tasks.infra.config;

import com.edu.silva.tasks.domain.enums.TaskSource;

import java.util.UUID;

public class UserContext {
    private static final ThreadLocal<UUID> currentUser = new ThreadLocal<>();
    private static final ThreadLocal<TaskSource> currentSource = new ThreadLocal<>();

    public static void setUser(UUID userId) {
        currentUser.set(userId);
    }

    public static UUID getUser() {
        return currentUser.get();
    }

    /**
     * Aplicação dona da requisição (StudyFlow ou Orçamentos), resolvida pelo gateway.
     * Sem a informação, mantém o comportamento anterior (StudyFlow).
     */
    public static void setSource(TaskSource source) {
        if (source == null) {
            currentSource.remove();
            return;
        }
        currentSource.set(source);
    }

    public static TaskSource getSource() {
        TaskSource source = currentSource.get();
        return source == null ? TaskSource.STUDY : source;
    }

    public static void clear() {
        currentUser.remove();
        currentSource.remove();
    }
}
