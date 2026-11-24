package com.silva.edu.finances.controllers;

import com.edu.silva.common.DefaultResponse;
import com.silva.edu.finances.domain.DashboardHorizon;
import com.silva.edu.finances.domain.dtos.response.DashboardPendingResponse;
import com.silva.edu.finances.domain.dtos.response.DashboardSummaryResponse;
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

import java.time.LocalDate;
import java.time.ZoneOffset;

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
            @RequestParam(required = false) String month,
            @RequestParam(defaultValue = "30") @Min(1) @Max(365) int days) {
        int horizonDays = horizon(month, days);
        return ResponseEntity.ok(new DefaultResponse<>("Pending items found successfully", service.pending(accountId, horizonDays)));
    }

    @GetMapping("/dashboard/investments")
    public ResponseEntity<DefaultResponse<InvestmentSummaryResponse>> investments(
            @RequestParam @NotBlank String accountId) {
        return ResponseEntity.ok(new DefaultResponse<>("Investment summary found successfully", service.investments(accountId)));
    }

    @GetMapping("/dashboard/summary")
    public ResponseEntity<DefaultResponse<DashboardSummaryResponse>> summary(
            @RequestParam @NotBlank String accountId,
            @RequestParam(required = false) String month,
            @RequestParam(defaultValue = "90") @Min(30) @Max(180) int days) {
        int horizonDays = horizon(month, days);
        return ResponseEntity.ok(new DefaultResponse<>("Dashboard summary calculated", service.summary(accountId, horizonDays)));
    }

    /** Quando o mês é informado, o horizonte vai até o último dia dele; senão vale `days`. */
    private static int horizon(String month, int days) {
        if (month == null || month.isBlank()) return days;
        return DashboardHorizon.daysUntilEndOfMonth(month, LocalDate.now(ZoneOffset.UTC));
    }
}
