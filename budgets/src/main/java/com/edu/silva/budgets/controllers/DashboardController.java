package com.edu.silva.budgets.controllers;

import com.edu.silva.common.DefaultResponse;
import com.edu.silva.budgets.domain.dtos.responses.DashboardKanbanResponseDTO;
import com.edu.silva.budgets.domain.dtos.responses.DashboardResponseDTO;
import com.edu.silva.budgets.services.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DashboardController {
    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DefaultResponse<DashboardResponseDTO>> getData() {
        DashboardResponseDTO dto = service.getData();
        return ResponseEntity.ok(
                new DefaultResponse<>("Find data successfully", dto)
        );
    }

    @GetMapping("/dashboard/kanban")
    public ResponseEntity<DefaultResponse<DashboardKanbanResponseDTO>> getDataKanban() {
        DashboardKanbanResponseDTO dto = service.getDataKanban();
        return ResponseEntity.ok(
                new DefaultResponse<>("Find data successfully", dto)
        );
    }
}
