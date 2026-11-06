package com.silva.edu.finances.domain.dtos.response;

import com.silva.edu.finances.domain.enums.InvestmentMovementType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InvestmentMovementResponse(UUID id, String accountId, InvestmentMovementType type,
                                         BigDecimal amount, LocalDate date, String description) {
}
