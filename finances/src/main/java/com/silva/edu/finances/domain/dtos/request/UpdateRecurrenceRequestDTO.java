package com.silva.edu.finances.domain.dtos.request;

import com.silva.edu.finances.domain.enums.RecurrenceStatus;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateRecurrenceRequestDTO(
        @Min(1) @Max(31) int day,
        @NotBlank String title,
        @Min(0) Integer installments,
        @Min(0) Integer payed,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 15, fraction = 4) BigDecimal value,
        UUID categoryID,
        Boolean income,
        RecurrenceStatus status
) {
}
