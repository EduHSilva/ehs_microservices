package com.silva.edu.finances.domain.dtos.request;

import com.silva.edu.finances.domain.enums.InvestmentMovementType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvestmentMovementRequest(
        @NotBlank @Size(max = 80) String accountId,
        @NotNull InvestmentMovementType type,
        @NotNull @DecimalMin("0.01") @Digits(integer = 15, fraction = 4) BigDecimal amount,
        @NotNull LocalDate date,
        @Size(max = 160) String description
) {
}
