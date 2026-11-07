package com.edu.silva.notifications.domain.dtos;

import java.time.LocalDateTime;

public record QueueStatsDTO(
        String queue,
        long readyMessages,
        long consumers,
        long sent,
        long errors,
        LocalDateTime lastErrorAt,
        String lastErrorTo
) {
}
