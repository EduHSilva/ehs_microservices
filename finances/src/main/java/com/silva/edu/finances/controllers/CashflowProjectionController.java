package com.silva.edu.finances.controllers;

import com.edu.silva.common.DefaultResponse;
import com.silva.edu.finances.domain.dtos.response.CashflowProjectionResponse;
import com.silva.edu.finances.services.CashflowProjectionService;
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
public class CashflowProjectionController {
    private final CashflowProjectionService service;
    public CashflowProjectionController(CashflowProjectionService service) { this.service = service; }

    @GetMapping("/dashboard/projection")
    public ResponseEntity<DefaultResponse<CashflowProjectionResponse>> project(
            @RequestParam @NotBlank String accountId,
            @RequestParam(defaultValue = "90") @Min(30) @Max(180) int days) {
        return ResponseEntity.ok(new DefaultResponse<>("Cashflow projection calculated", service.project(accountId, days)));
    }
}
