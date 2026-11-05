package com.silva.edu.finances.domain.dtos.request;

import com.silva.edu.finances.domain.enums.FinancialAccountType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CreateFinanceAccountRequest(@NotBlank @Size(max=120) String name,
        @NotNull FinancialAccountType type,
        @NotNull @Digits(integer=15, fraction=4) BigDecimal openingBalance) {}
