package com.silva.edu.finances.services.impl;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardServiceImplTest {

    @Test
    void livingCostAverageIsNullWithLessThanThreeMonths() {
        assertThat(DashboardServiceImpl.livingCostAverage(BigDecimal.valueOf(1000), 0)).isNull();
        assertThat(DashboardServiceImpl.livingCostAverage(BigDecimal.valueOf(2000), 2)).isNull();
    }

    @Test
    void livingCostAverageDividesByMonthsWithData() {
        assertThat(DashboardServiceImpl.livingCostAverage(BigDecimal.valueOf(3000), 3))
                .isEqualByComparingTo(BigDecimal.valueOf(1000));
        assertThat(DashboardServiceImpl.livingCostAverage(BigDecimal.valueOf(2500), 5))
                .isEqualByComparingTo(BigDecimal.valueOf(500));
    }

    @Test
    void livingCostAverageUsesFullWindowWhenSixMonthsHaveData() {
        assertThat(DashboardServiceImpl.livingCostAverage(BigDecimal.valueOf(6000), 6))
                .isEqualByComparingTo(BigDecimal.valueOf(1000));
    }

    @Test
    void investmentAverageDividesByMonthsWithData() {
        assertThat(DashboardServiceImpl.perMonthAverage(BigDecimal.valueOf(500), 1, 4))
                .isEqualByComparingTo(BigDecimal.valueOf(500));
        assertThat(DashboardServiceImpl.perMonthAverage(BigDecimal.valueOf(3000), 3, 4))
                .isEqualByComparingTo(BigDecimal.valueOf(1000));
    }

    @Test
    void investmentAverageIsZeroWithoutData() {
        assertThat(DashboardServiceImpl.perMonthAverage(BigDecimal.valueOf(0), 0, 4))
                .isZero();
    }
}
