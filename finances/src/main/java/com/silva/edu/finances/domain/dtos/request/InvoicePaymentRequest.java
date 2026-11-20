package com.silva.edu.finances.domain.dtos.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvoicePaymentRequest(
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 15, fraction = 4) BigDecimal value,
        @NotNull LocalDate date
) {
}
