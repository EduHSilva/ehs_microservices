package com.silva.edu.finances.domain.dtos.response;

import java.math.BigDecimal;
import java.util.UUID;

public record MonthlyGoalSummaryResponse(UUID goalId, String accountId, String title, String periodMonth,
                                         BigDecimal incomeBase, BigDecimal expected, BigDecimal realized,
                                         BigDecimal difference, BigDecimal progress) {
}
