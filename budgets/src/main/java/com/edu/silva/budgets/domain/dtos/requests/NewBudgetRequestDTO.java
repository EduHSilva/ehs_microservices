package com.edu.silva.budgets.domain.dtos.requests;

import com.edu.silva.budgets.domain.dtos.ItemDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record NewBudgetRequestDTO(
        @NotBlank String title,
        @NotNull UUID client,
        int validate,
        @NotBlank String description,
        @NotEmpty List<ItemDTO> items,
        String terms,
        String observations
) {
}
