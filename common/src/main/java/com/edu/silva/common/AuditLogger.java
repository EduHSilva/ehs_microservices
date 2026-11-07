package com.edu.silva.common;

import org.slf4j.LoggerFactory;

import java.util.Map;

public final class AuditLogger {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger("AUDIT");

    private AuditLogger() {
    }

    public static void log(
            String actor,
            String action,
            String entity,
            Object entityId,
            Map<String, ?> details
    ) {
        log.info(
                "AUDIT actor={} action={} entity={} entityId={} details={}",
                actor,
                action,
                entity,
                entityId,
                details
        );
    }
}
