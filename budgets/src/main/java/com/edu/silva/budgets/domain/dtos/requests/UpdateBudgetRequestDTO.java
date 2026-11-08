package com.edu.silva.budgets.domain.dtos.requests;

import com.edu.silva.budgets.domain.dtos.ItemDTO;
import com.edu.silva.budgets.domain.enums.BudgetStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateBudgetRequestDTO(
        @NotBlank String title,
        String description,
        int validate,
        @NotNull List<ItemDTO> items,
        String terms,
        String observations,
        BudgetStatus status,
        Integer kanbanOrder
) {
}
