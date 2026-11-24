package com.silva.edu.finances.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DashboardHorizonTest {

    @Test
    void countsDaysUntilEndOfCurrentMonth() {
        assertThat(DashboardHorizon.daysUntilEndOfMonth("2026-11", LocalDate.of(2026, 11, 24)))
                .isEqualTo(6);
    }

    @Test
    void isZeroOnTheLastDayOfTheMonth() {
        assertThat(DashboardHorizon.daysUntilEndOfMonth("2026-11", LocalDate.of(2026, 11, 30)))
                .isZero();
    }

    @Test
    void reachesTheEndOfAFutureMonth() {
        assertThat(DashboardHorizon.daysUntilEndOfMonth("2026-12", LocalDate.of(2026, 11, 24)))
                .isEqualTo(37);
    }

    @Test
    void rejectsPastMonths() {
        assertThatThrownBy(() -> DashboardHorizon.daysUntilEndOfMonth("2026-10", LocalDate.of(2026, 11, 24)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsInvalidMonthFormat() {
        assertThatThrownBy(() -> DashboardHorizon.daysUntilEndOfMonth("novembro", LocalDate.of(2026, 11, 24)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
