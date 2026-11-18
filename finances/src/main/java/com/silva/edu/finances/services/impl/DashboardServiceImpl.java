package com.silva.edu.finances.services.impl;

import com.silva.edu.finances.domain.dtos.response.DashboardPendingResponse;
import com.silva.edu.finances.domain.dtos.response.DashboardSummaryResponse;
import com.silva.edu.finances.domain.dtos.response.InvestmentSummaryResponse;
import com.silva.edu.finances.domain.entities.Recurrence;
import com.silva.edu.finances.domain.enums.CategoryClassification;
import com.silva.edu.finances.domain.enums.RecurrenceStatus;
import com.silva.edu.finances.domain.enums.TransactionStatus;
import com.silva.edu.finances.repositories.GoalContributionRepository;
import com.silva.edu.finances.repositories.MonthlyGoalRepository;
import com.silva.edu.finances.repositories.RecurrenceRepository;
import com.silva.edu.finances.repositories.TransactionRepository;
import com.silva.edu.finances.services.CashflowProjectionService;
import com.silva.edu.finances.services.CategoryService;
import com.silva.edu.finances.services.DashboardService;
import com.silva.edu.finances.services.FinanceAccountService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class DashboardServiceImpl implements DashboardService {
    private static final ZoneOffset UTC = ZoneOffset.UTC;
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final int LIVING_COST_MONTHS = 6;
    private static final int INVESTMENT_AVERAGE_MONTHS = 3;
    private static final int TOP_CATEGORIES = 5;

    private final FinanceAccountService accounts;
    private final TransactionRepository transactions;
    private final RecurrenceRepository recurrences;
    private final CategoryService categories;
    private final RecurrenceMaterializer materializer;
    private final CashflowProjectionService projections;
    private final MonthlyGoalRepository goals;
    private final GoalContributionRepository contributions;

    public DashboardServiceImpl(FinanceAccountService accounts, TransactionRepository transactions,
                                RecurrenceRepository recurrences, CategoryService categories,
                                RecurrenceMaterializer materializer, CashflowProjectionService projections,
                                MonthlyGoalRepository goals, GoalContributionRepository contributions) {
        this.accounts = accounts;
        this.transactions = transactions;
        this.recurrences = recurrences;
        this.categories = categories;
        this.materializer = materializer;
        this.projections = projections;
        this.goals = goals;
        this.contributions = contributions;
    }

    @Override
    @Transactional
    public DashboardPendingResponse pending(String accountId, int horizonDays) {
        accounts.requireAccess(accountId);
        materializer.materialize(accountId);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate end = today.plusDays(horizonDays);
        Date to = Date.from(end.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant());

        List<DashboardPendingResponse.Item> items = new ArrayList<>();
        for (var transaction : transactions.findAllByAccountIdAndStatusAndExecutionDateLessThanEqualOrderByExecutionDateAsc(
                accountId, TransactionStatus.PENDING, to)) {
            LocalDate date = transaction.getExecutionDate().toInstant().atZone(ZoneOffset.UTC).toLocalDate();
            items.add(new DashboardPendingResponse.Item(transaction.getId(), "TRANSACTION", transaction.getTitle(),
                    transaction.getValue(), transaction.isIncome(), date, transaction.getStatus().name()));
        }
        for (Recurrence recurrence : recurrences.findAllByAccountIdAndStatus(accountId, RecurrenceStatus.PENDING)) {
            LocalDate date = nextDate(today, recurrence.getDay());
            if (date.isAfter(end)) continue;
            items.add(new DashboardPendingResponse.Item(recurrence.getId(), "RECURRENCE", recurrence.getTitle(),
                    recurrence.getValue(), recurrence.isIncome(), date, recurrence.getStatus().name()));
        }
        items.sort(Comparator.comparing(DashboardPendingResponse.Item::date)
                .thenComparing(DashboardPendingResponse.Item::title, String.CASE_INSENSITIVE_ORDER));
        return new DashboardPendingResponse(accountId, horizonDays, items);
    }

    @Override
    @Transactional
    public InvestmentSummaryResponse investments(String accountId) {
        accounts.requireAccess(accountId);
        var account = accounts.get(accountId);
        UUID owner = accounts.ownerOf(accountId);
        categories.ensureInvestmentCategory(owner);
        BigDecimal contributions = BigDecimal.ZERO;
        BigDecimal withdrawals = BigDecimal.ZERO;
        for (Object[] row : transactions.sumByClassification(owner, accountId,
                CategoryClassification.INVESTMENT, new Date())) {
            BigDecimal total = (BigDecimal) row[1];
            if (Boolean.TRUE.equals(row[0])) withdrawals = total;
            else contributions = total;
        }
        return new InvestmentSummaryResponse(accountId, contributions, withdrawals,
                contributions.subtract(withdrawals), account.currency());
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary(String accountId, int horizonDays) {
        accounts.requireAccess(accountId);
        UUID owner = accounts.ownerOf(accountId);
        LocalDate today = LocalDate.now(UTC);
        YearMonth month = YearMonth.from(today);
        Date monthStart = atStartOfDay(month.atDay(1));
        Date monthToDateEnd = atStartOfDay(today.plusDays(1));
        var projection = projections.project(accountId, horizonDays);

        BigDecimal monthIncome = ZERO;
        BigDecimal monthExpenses = ZERO;
        for (Object[] row : transactions.sumByIncomeBetween(owner, accountId, monthStart, monthToDateEnd)) {
            BigDecimal total = (BigDecimal) row[1];
            if (Boolean.TRUE.equals(row[0])) monthIncome = total;
            else monthExpenses = total;
        }
        BigDecimal monthNet = monthIncome.subtract(monthExpenses);

        BigDecimal monthContributions = ZERO;
        BigDecimal monthWithdrawals = ZERO;
        for (Object[] row : transactions.sumByClassificationBetween(owner, accountId,
                CategoryClassification.INVESTMENT, monthStart, monthToDateEnd)) {
            BigDecimal total = (BigDecimal) row[1];
            if (Boolean.TRUE.equals(row[0])) monthWithdrawals = total;
            else monthContributions = total;
        }

        BigDecimal investmentBalance = ZERO;
        for (Object[] row : transactions.sumByClassification(owner, accountId,
                CategoryClassification.INVESTMENT, new Date())) {
            BigDecimal total = (BigDecimal) row[1];
            investmentBalance = Boolean.TRUE.equals(row[0])
                    ? investmentBalance.subtract(total) : investmentBalance.add(total);
        }
        BigDecimal initialBalances = nz(goals.sumInitialBalances(owner, accountId));
        BigDecimal adjustments = nz(contributions.sumAdjustments(owner, accountId, today.plusDays(1)));
        BigDecimal investmentTotal = investmentBalance.add(initialBalances).add(adjustments);

        Date averageStart = atStartOfDay(month.minusMonths(INVESTMENT_AVERAGE_MONTHS).atDay(1));
        BigDecimal averageNet = ZERO;
        for (Object[] row : transactions.sumByClassificationBetween(owner, accountId,
                CategoryClassification.INVESTMENT, averageStart, monthStart)) {
            BigDecimal total = (BigDecimal) row[1];
            averageNet = Boolean.TRUE.equals(row[0])
                    ? averageNet.subtract(total) : averageNet.add(total);
        }
        BigDecimal monthlyInvestmentNet = averageNet.divide(
                BigDecimal.valueOf(INVESTMENT_AVERAGE_MONTHS), 4, RoundingMode.HALF_UP);
        BigDecimal monthsRemaining = BigDecimal.valueOf(horizonDays)
                .divide(BigDecimal.valueOf(30), 4, RoundingMode.HALF_UP);
        BigDecimal investmentProjection = investmentTotal
                .add(monthlyInvestmentNet.multiply(monthsRemaining))
                .setScale(2, RoundingMode.HALF_UP);
        if (investmentProjection.signum() < 0) investmentProjection = ZERO.setScale(2, RoundingMode.HALF_UP);

        Date livingCostStart = atStartOfDay(month.minusMonths(LIVING_COST_MONTHS).atDay(1));
        BigDecimal essentialExpenses = ZERO;
        for (Object[] row : transactions.sumByClassificationBetween(owner, accountId,
                CategoryClassification.ESSENTIAL, livingCostStart, monthStart)) {
            if (!Boolean.TRUE.equals(row[0])) essentialExpenses = (BigDecimal) row[1];
        }
        BigDecimal livingCost = essentialExpenses.divide(
                BigDecimal.valueOf(LIVING_COST_MONTHS), 2, RoundingMode.HALF_UP);

        List<DashboardSummaryResponse.CategoryShare> topCategories = new ArrayList<>();
        for (Object[] row : transactions.topExpenseCategories(owner, accountId, monthStart, monthToDateEnd,
                PageRequest.of(0, TOP_CATEGORIES))) {
            BigDecimal amount = (BigDecimal) row[3];
            BigDecimal share = monthExpenses.signum() == 0 ? ZERO
                    : amount.multiply(BigDecimal.valueOf(100)).divide(monthExpenses, 1, RoundingMode.HALF_UP);
            topCategories.add(new DashboardSummaryResponse.CategoryShare(
                    row[0] == null ? null : row[0].toString(), (String) row[1], (String) row[2], amount, share));
        }

        return new DashboardSummaryResponse(accountId, horizonDays, Instant.now(),
                new DashboardSummaryResponse.Cash(projection.currentBalance(), monthIncome, monthExpenses,
                        monthNet, projection.closingBalance()),
                new DashboardSummaryResponse.Investments(investmentTotal, monthContributions, monthWithdrawals,
                        monthContributions.subtract(monthWithdrawals), investmentProjection),
                new DashboardSummaryResponse.NetWorth(
                        projection.currentBalance().add(investmentTotal),
                        projection.closingBalance().add(investmentProjection)),
                new DashboardSummaryResponse.LivingCost(livingCost, LIVING_COST_MONTHS, essentialExpenses),
                topCategories);
    }

    private static Date atStartOfDay(LocalDate date) {
        return Date.from(date.atStartOfDay(UTC).toInstant());
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private LocalDate nextDate(LocalDate today, int day) {
        YearMonth month = YearMonth.from(today);
        LocalDate candidate = month.atDay(Math.min(day, month.lengthOfMonth()));
        if (candidate.isBefore(today)) {
            YearMonth next = month.plusMonths(1);
            candidate = next.atDay(Math.min(day, next.lengthOfMonth()));
        }
        return candidate;
    }
}
