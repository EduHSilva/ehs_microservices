package com.silva.edu.finances.domain.dtos.request;

import com.silva.edu.finances.domain.enums.TransactionStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

public record UpdateTransactionRequestDTO(
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 15, fraction = 4) BigDecimal value,
        @NotBlank String title,
        Date executionDate,
        UUID categoryID,
        Boolean income,
        TransactionStatus status
) {

}
