package com.silva.edu.finances.domain.dtos.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateFinanceAccountRequest(
        @NotBlank @Size(max = 120) String name,
        @DecimalMin("0.00") @Digits(integer = 15, fraction = 4) BigDecimal openingBalance) {}
