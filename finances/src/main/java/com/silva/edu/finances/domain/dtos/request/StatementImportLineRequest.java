package com.silva.edu.finances.domain.dtos.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record StatementImportLineRequest(
        @NotBlank @Size(max = 300) String description,
        @NotNull @DecimalMin("0.01") @Digits(integer = 15, fraction = 4) BigDecimal amount,
        boolean income,
        @NotNull LocalDate date,
        UUID categoryId,
        @Size(max = 160) String sourceTransactionId
) {}
