package com.edu.silva.budgets.domain.dtos.requests;

import com.edu.silva.budgets.domain.enums.BudgetStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Decisão do cliente pelo link público: apenas {@code APPROVED} ou {@code DENY}.
 */
public record PublicBudgetDecisionRequestDTO(
        @NotNull BudgetStatus status
) {
}
