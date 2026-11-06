package com.silva.edu.finances.domain.dtos.request;

import com.silva.edu.finances.domain.enums.RecurrenceStatus;
import com.silva.edu.finances.domain.enums.RecurrenceType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record AddRecurrenceRequestDTO(
        @Min(1) @Max(31) int day,
        boolean income,
        @NotNull UUID categoryID,
        @NotNull RecurrenceType type,
        @Min(0) Integer installments,
        @Min(0) Integer payed,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 15, fraction = 4) BigDecimal value,
        @NotBlank String title,
        @Size(max = 80) String accountId,
        RecurrenceStatus status
) {
}
