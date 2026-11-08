package com.edu.silva.budgets.controllers;

import com.edu.silva.common.DefaultResponse;
import com.edu.silva.common.PageResponse;
import com.edu.silva.budgets.domain.dtos.requests.NewBudgetRequestDTO;
import com.edu.silva.budgets.domain.dtos.requests.UpdateBudgetRequestDTO;
import com.edu.silva.budgets.domain.dtos.responses.BudgetResponseDTO;
import com.edu.silva.budgets.services.BudgetService;
import jakarta.validation.Valid;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class BudgetController {

    private final String BASE_URL = "/budgets/budgets";

    private final BudgetService service;

    public BudgetController(BudgetService service) {
        this.service = service;
    }

    @GetMapping("/budgets")
    public ResponseEntity<@NonNull DefaultResponse<PageResponse<BudgetResponseDTO>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String status
    ) {
        Page<@NonNull BudgetResponseDTO> budgets = service.findAll(page, size, title, status);

        return ResponseEntity.ok(
                DefaultResponse.paged("Find all budgets successfully", budgets, BASE_URL)
        );
    }

    @PostMapping("/budgets")
    public ResponseEntity<@NonNull DefaultResponse<BudgetResponseDTO>> add(
            @RequestBody @Valid NewBudgetRequestDTO dto
    ) {
        BudgetResponseDTO budget = service.save(dto);

        return ResponseEntity.ok(
                new DefaultResponse<>("Budget successfully saved", budget, BASE_URL, budget.getId())
        );
    }

    @GetMapping("/budgets/{id}")
    public ResponseEntity<@NonNull DefaultResponse<BudgetResponseDTO>> get(
            @PathVariable UUID id
    ) {
        BudgetResponseDTO dto = service.findById(id);
        return ResponseEntity.ok(
                new DefaultResponse<>("Budget successfully retrieved", dto, BASE_URL, id)
        );
    }

    @DeleteMapping("/budgets/{id}")
    public ResponseEntity<@NonNull DefaultResponse<Void>> delete(
            @PathVariable UUID id
    ) {
        service.delete(id);
        return ResponseEntity.ok(
                new DefaultResponse<Void>("Budget successfully deleted")
        );
    }

    @PutMapping("/budgets/{id}")
    public ResponseEntity<@NonNull DefaultResponse<BudgetResponseDTO>> update(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateBudgetRequestDTO dto
    ) {
        return ResponseEntity.ok(
                new DefaultResponse<>("Budget successfully saved", service.update(id, dto), BASE_URL, id)
        );
    }
}
