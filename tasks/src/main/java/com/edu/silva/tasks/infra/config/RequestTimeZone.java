package com.edu.silva.tasks.infra.config;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Time zone the client is using (X-Time-Zone header). Calendar days are resolved with the
 * same clock the user sees, otherwise a task created late in the evening is stored as the
 * next day on a UTC server and completions for "today" are rejected.
 */
public final class RequestTimeZone {
    private static final ThreadLocal<ZoneId> current = new ThreadLocal<>();

    private RequestTimeZone() {
    }

    public static void set(String zoneId) {
        if (zoneId == null || zoneId.isBlank()) {
            current.remove();
            return;
        }
        try {
            current.set(ZoneId.of(zoneId));
        } catch (Exception ignored) {
            current.remove();
        }
    }

    public static ZoneId get() {
        ZoneId zone = current.get();
        return zone == null ? ZoneId.systemDefault() : zone;
    }

    public static LocalDate today() {
        return LocalDate.now(get());
    }

    public static void clear() {
        current.remove();
    }
}
