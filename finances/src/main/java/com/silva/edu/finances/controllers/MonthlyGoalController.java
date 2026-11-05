package com.silva.edu.finances.controllers;

import com.edu.silva.common.DefaultResponse;
import com.edu.silva.common.PageResponse;
import com.silva.edu.finances.domain.dtos.request.GoalContributionRequest;
import com.silva.edu.finances.domain.dtos.request.MonthlyGoalRequest;
import com.silva.edu.finances.domain.dtos.response.MonthlyGoalResponse;
import com.silva.edu.finances.domain.dtos.response.MonthlyGoalSummaryResponse;
import com.silva.edu.finances.services.MonthlyGoalService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@Validated
public class MonthlyGoalController {
    private static final String BASE_URL = "/goals";
    private final MonthlyGoalService service;

    public MonthlyGoalController(MonthlyGoalService service) { this.service = service; }

    @GetMapping(BASE_URL)
    public ResponseEntity<DefaultResponse<PageResponse<MonthlyGoalResponse>>> list(
            @RequestParam @NotBlank String accountId,
            @RequestParam @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])") String periodMonth,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size) {
        return ResponseEntity.ok(DefaultResponse.paged("Goals found successfully",
                service.list(accountId, periodMonth, page, size), BASE_URL));
    }

    @PostMapping(BASE_URL)
    public ResponseEntity<DefaultResponse<MonthlyGoalResponse>> create(@RequestBody @Valid MonthlyGoalRequest request) {
        MonthlyGoalResponse goal = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new DefaultResponse<>("Goal created successfully", goal));
    }

    @PutMapping(BASE_URL + "/{id}")
    public ResponseEntity<DefaultResponse<MonthlyGoalResponse>> update(@PathVariable UUID id,
            @RequestBody @Valid MonthlyGoalRequest request) {
        return ResponseEntity.ok(new DefaultResponse<>("Goal updated successfully", service.update(id, request)));
    }

    @DeleteMapping(BASE_URL + "/{id}")
    public ResponseEntity<DefaultResponse<Void>> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.ok(new DefaultResponse<>("Goal deleted successfully"));
    }

    @GetMapping(BASE_URL + "/{id}/summary")
    public ResponseEntity<DefaultResponse<MonthlyGoalSummaryResponse>> summary(@PathVariable UUID id,
            @RequestParam @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])") String periodMonth) {
        return ResponseEntity.ok(new DefaultResponse<>("Goal summary found successfully", service.summary(id, periodMonth)));
    }

    @PostMapping(BASE_URL + "/{id}/contributions")
    public ResponseEntity<DefaultResponse<Void>> contribute(@PathVariable UUID id,
            @RequestBody @Valid GoalContributionRequest request) {
        service.contribute(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new DefaultResponse<>("Contribution registered successfully"));
    }

    @DeleteMapping("/goal-contributions/{id}")
    public ResponseEntity<DefaultResponse<Void>> removeContribution(@PathVariable UUID id) {
        service.removeContribution(id);
        return ResponseEntity.ok(new DefaultResponse<>("Contribution removed successfully"));
    }
}
