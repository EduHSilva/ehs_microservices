package com.silva.edu.finances.controllers;

import com.edu.silva.common.DefaultResponse;
import com.silva.edu.finances.domain.dtos.request.InvestmentMovementRequest;
import com.silva.edu.finances.domain.dtos.response.InvestmentMovementResponse;
import com.silva.edu.finances.domain.dtos.response.InvestmentSummaryResponse;
import com.silva.edu.finances.services.InvestmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@Validated
public class InvestmentController {
    private static final String BASE_URL = "/investments";
    private final InvestmentService service;

    public InvestmentController(InvestmentService service) {
        this.service = service;
    }

    @GetMapping(BASE_URL)
    public ResponseEntity<DefaultResponse<List<InvestmentMovementResponse>>> list(
            @RequestParam @NotBlank String accountId) {
        return ResponseEntity.ok(new DefaultResponse<>("Investment movements found successfully", service.list(accountId)));
    }

    @GetMapping(BASE_URL + "/summary")
    public ResponseEntity<DefaultResponse<InvestmentSummaryResponse>> summary(
            @RequestParam @NotBlank String accountId) {
        return ResponseEntity.ok(new DefaultResponse<>("Investment summary found successfully", service.summary(accountId)));
    }

    @PostMapping(BASE_URL)
    public ResponseEntity<DefaultResponse<InvestmentMovementResponse>> create(
            @RequestBody @Valid InvestmentMovementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new DefaultResponse<>("Investment movement created successfully", service.create(request)));
    }

    @DeleteMapping(BASE_URL + "/{id}")
    public ResponseEntity<DefaultResponse<Void>> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.ok(new DefaultResponse<>("Investment movement deleted successfully"));
    }
}
