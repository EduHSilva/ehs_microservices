package com.edu.silva.notifications.controllers;

import com.edu.silva.common.DefaultResponse;
import com.edu.silva.notifications.domain.dtos.QueueStatsDTO;
import com.edu.silva.notifications.services.QueueStatsService;
import lombok.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class QueueStatsController {

    private final QueueStatsService service;

    public QueueStatsController(QueueStatsService service) {
        this.service = service;
    }

    @GetMapping("/queue-stats")
    public ResponseEntity<@NonNull DefaultResponse<QueueStatsDTO>> stats() {
        return ResponseEntity.ok(new DefaultResponse<>(
                "Email queue statistics successfully retrieved",
                service.stats()
        ));
    }
}
