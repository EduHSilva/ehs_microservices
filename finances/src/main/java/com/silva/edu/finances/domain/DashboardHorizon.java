package com.silva.edu.finances.domain;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

/**
 * Horizonte do dashboard expresso em meses: número de dias de hoje até o último dia
 * do mês escolhido (0 quando o mês fecha hoje). O mês precisa ser o corrente ou futuro.
 */
public final class DashboardHorizon {
    private static final int MAX_MONTHS_AHEAD = 12;
    private static final int MAX_DAYS = 366;

    private DashboardHorizon() {
    }

    public static int daysUntilEndOfMonth(String month, LocalDate today) {
        YearMonth selected;
        try {
            selected = YearMonth.parse(month);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Month must be in the YYYY-MM format");
        }
        YearMonth current = YearMonth.from(today);
        if (selected.isBefore(current))
            throw new IllegalArgumentException("Month must be the current month or a future month");
        if (selected.isAfter(current.plusMonths(MAX_MONTHS_AHEAD)))
            throw new IllegalArgumentException("Month is too far ahead");
        long days = ChronoUnit.DAYS.between(today, selected.atEndOfMonth());
        return (int) Math.max(0, Math.min(days, MAX_DAYS));
    }
}
