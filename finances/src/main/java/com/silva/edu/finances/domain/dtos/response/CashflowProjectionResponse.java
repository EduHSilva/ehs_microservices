package com.silva.edu.finances.domain.dtos.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record CashflowProjectionResponse(String accountId, int horizonDays, Instant calculatedAt,
        BigDecimal currentBalance, BigDecimal closingBalance, String confidence,
        List<String> assumptions, List<Point> series, List<Item> items) {
    public record Point(LocalDate date, BigDecimal balance, BigDecimal knownIncome, BigDecimal knownExpenses,
                        BigDecimal estimatedIncome, BigDecimal estimatedExpenses) {}
    public record Item(LocalDate date, String title, BigDecimal amount, boolean income, String kind) {}
}
