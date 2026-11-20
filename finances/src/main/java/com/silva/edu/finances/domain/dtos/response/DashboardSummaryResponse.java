package com.silva.edu.finances.domain.dtos.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record DashboardSummaryResponse(
        String accountId,
        int horizonDays,
        Instant calculatedAt,
        Cash cash,
        Investments investments,
        NetWorth netWorth,
        LivingCost livingCost,
        List<CategoryShare> topCategories) {

    public record Cash(BigDecimal currentBalance, BigDecimal monthIncome, BigDecimal monthExpenses,
                       BigDecimal monthNet, BigDecimal projectedClosingBalance) {}

    public record Investments(BigDecimal total, BigDecimal monthContributions, BigDecimal monthWithdrawals,
                              BigDecimal monthNet, BigDecimal projectedClosingBalance) {}

    public record NetWorth(BigDecimal current, BigDecimal projected) {}

    // months = meses com dados usados na média; monthlyAverage é nulo quando months < 3.
    public record LivingCost(BigDecimal monthlyAverage, int months, BigDecimal total, Reserve reserve) {}

    public record Reserve(String goalId, String title, BigDecimal total, BigDecimal months) {}

    public record CategoryShare(String categoryId, String title, String color, BigDecimal amount, BigDecimal share) {}
}
