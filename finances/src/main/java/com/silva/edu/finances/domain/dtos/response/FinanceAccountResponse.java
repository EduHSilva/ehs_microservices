package com.silva.edu.finances.domain.dtos.response;

import com.silva.edu.finances.domain.enums.FinancialAccountType;
import java.math.BigDecimal;

public record FinanceAccountResponse(String id, String name, FinancialAccountType type,
        BigDecimal openingBalance, BigDecimal balance, String currency) {}
