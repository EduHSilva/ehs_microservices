package com.silva.edu.finances.domain.dtos.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;

/**
 * Optional payload for confirming a pending transaction. When {@code value} is provided the
 * transaction amount is updated before it is confirmed.
 */
public record ConfirmTransactionRequest(
        @DecimalMin(value = "0.01") @Digits(integer = 15, fraction = 4) BigDecimal value
) {
}
