package com.silva.edu.finances.domain.dtos.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record UpdateCreditCardRequest(
        @NotBlank @Size(max = 120) String name,
        @DecimalMin("0") @Digits(integer = 15, fraction = 4) BigDecimal cardLimit,
        @Min(1) @Max(31) int closingDay,
        @Min(1) @Max(31) int dueDay
) {
}
