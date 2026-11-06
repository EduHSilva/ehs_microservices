package com.silva.edu.finances.domain.dtos.request;

import com.silva.edu.finances.domain.enums.GoalMode;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MonthlyGoalRequest(
        @NotBlank @Size(max = 120) String title,
        @NotBlank @Size(max = 80) String accountId,
        @NotNull GoalMode mode,
        @NotNull @DecimalMin("0.01") @Digits(integer = 15, fraction = 4) BigDecimal amount,
        @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])") String periodMonth,
        LocalDate dtInicio,
        LocalDate dtFim,
        boolean active
) {
}
