package com.silva.edu.finances.services.impl;

import com.silva.edu.finances.domain.dtos.request.GoalContributionRequest;
import com.silva.edu.finances.domain.dtos.response.MonthlyGoalSummaryResponse;
import com.silva.edu.finances.domain.entities.GoalContribution;
import com.silva.edu.finances.domain.entities.MonthlyGoal;
import com.silva.edu.finances.domain.enums.GoalMode;
import com.silva.edu.finances.infra.config.UserContext;
import com.silva.edu.finances.repositories.GoalContributionRepository;
import com.silva.edu.finances.repositories.MonthlyGoalRepository;
import com.silva.edu.finances.repositories.TransactionRepository;
import com.silva.edu.finances.services.CategoryService;
import com.silva.edu.finances.services.FinanceAccountService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonthlyGoalServiceImplTest {
    @Mock private MonthlyGoalRepository goals;
    @Mock private GoalContributionRepository contributions;
    @Mock private TransactionRepository transactions;
    @Mock private FinanceAccountService accountService;
    @Mock private CategoryService categories;
    @InjectMocks private MonthlyGoalServiceImpl service;

    private final UUID userId = UUID.randomUUID();
    private final UUID goalId = UUID.randomUUID();
    private final String accountId = "PF-" + userId;

    @AfterEach
    void clearUser() {
        UserContext.clear();
    }

    @Test
    void adjustmentContributionDoesNotCreateTransaction() {
        UserContext.setUser(userId);
        MonthlyGoal goal = goal();
        when(goals.findByIdAndUserId(goalId, userId)).thenReturn(Optional.of(goal));

        service.contribute(goalId, new GoalContributionRequest(
                BigDecimal.valueOf(75), LocalDate.of(2026, 11, 10), null, true));

        ArgumentCaptor<GoalContribution> captor = ArgumentCaptor.forClass(GoalContribution.class);
        verify(contributions).save(captor.capture());
        GoalContribution saved = captor.getValue();
        assertThat(saved.isAdjustment()).isTrue();
        assertThat(saved.isAutoCreated()).isFalse();
        assertThat(saved.getTransactionId()).isNull();
        verify(transactions, never()).save(any());
    }

    @Test
    void summaryCountsAdjustmentsInSavedButNotInRealized() {
        UserContext.setUser(userId);
        MonthlyGoal goal = goal();
        goal.setInitialBalance(BigDecimal.valueOf(100));
        goal.setObjective(BigDecimal.valueOf(10000));
        when(goals.findByIdAndUserId(goalId, userId)).thenReturn(Optional.of(goal));
        when(transactions.sumConfirmedIncome(eq(userId), eq(accountId), any(), any()))
                .thenReturn(BigDecimal.valueOf(1000));
        when(contributions.sumRealizedForGoalAndPeriod(eq(goalId), eq(userId), any(), any()))
                .thenReturn(BigDecimal.valueOf(200));
        when(contributions.sumForGoalAndPeriod(eq(goalId), eq(userId), any(), any()))
                .thenReturn(BigDecimal.valueOf(500));

        MonthlyGoalSummaryResponse summary = service.summary(goalId, "2026-11");

        assertThat(summary.realized()).isEqualByComparingTo(BigDecimal.valueOf(200));
        assertThat(summary.progress()).isEqualByComparingTo(BigDecimal.valueOf(40));
        assertThat(summary.saved()).isEqualByComparingTo(BigDecimal.valueOf(600));
        assertThat(summary.objectiveProgress()).isEqualByComparingTo(BigDecimal.valueOf(6));
    }

    private MonthlyGoal goal() {
        MonthlyGoal goal = new MonthlyGoal();
        goal.setId(goalId);
        goal.setUserId(userId);
        goal.setAccountId(accountId);
        goal.setTitle("Reserva");
        goal.setMode(GoalMode.FIXED);
        goal.setAmount(BigDecimal.valueOf(500));
        goal.setPeriodMonth("2026-11");
        goal.setDtInicio(LocalDate.of(2026, 1, 1));
        return goal;
    }
}
