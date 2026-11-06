package com.silva.edu.finances.domain.dtos.response;

import com.silva.edu.finances.domain.enums.GoalMode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record MonthlyGoalResponse(UUID id, String accountId, String title, GoalMode mode, BigDecimal amount,
                                 String periodMonth, LocalDate dtInicio, LocalDate dtFim, boolean recurring,
                                 boolean active) {
}
