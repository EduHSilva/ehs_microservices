package com.silva.edu.finances.controllers;

import com.edu.silva.common.DefaultResponse;
import com.silva.edu.finances.domain.dtos.response.DashboardPendingResponse;
import com.silva.edu.finances.domain.dtos.response.InvestmentSummaryResponse;
import com.silva.edu.finances.services.DashboardService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
public class DashboardController {
    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/dashboard/pending")
    public ResponseEntity<DefaultResponse<DashboardPendingResponse>> pending(
            @RequestParam @NotBlank String accountId,
            @RequestParam(defaultValue = "30") @Min(1) @Max(365) int days) {
        return ResponseEntity.ok(new DefaultResponse<>("Pending items found successfully", service.pending(accountId, days)));
    }

    @GetMapping("/dashboard/investments")
    public ResponseEntity<DefaultResponse<InvestmentSummaryResponse>> investments(
            @RequestParam @NotBlank String accountId) {
        return ResponseEntity.ok(new DefaultResponse<>("Investment summary found successfully", service.investments(accountId)));
    }
}
