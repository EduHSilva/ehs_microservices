package com.silva.edu.finances.domain.dtos.request;

import com.silva.edu.finances.domain.enums.TransactionStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

public record AddTransactionRequestDTO(
        boolean income,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 15, fraction = 4) BigDecimal value,
        UUID categoryID,
        @NotBlank String title,
        UUID recurrencyID,
        @NotNull Date executionDate,
        @jakarta.validation.constraints.Size(max = 80) String accountId,
        TransactionStatus status
) {
}
