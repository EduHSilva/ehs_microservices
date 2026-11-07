package com.silva.edu.finances.services.impl;

import com.silva.edu.finances.domain.dtos.response.DashboardPendingResponse;
import com.silva.edu.finances.domain.dtos.response.InvestmentSummaryResponse;
import com.silva.edu.finances.domain.entities.Recurrence;
import com.silva.edu.finances.domain.enums.CategoryClassification;
import com.silva.edu.finances.domain.enums.RecurrenceStatus;
import com.silva.edu.finances.domain.enums.TransactionStatus;
import com.silva.edu.finances.infra.config.UserContext;
import com.silva.edu.finances.repositories.RecurrenceRepository;
import com.silva.edu.finances.repositories.TransactionRepository;
import com.silva.edu.finances.services.CategoryService;
import com.silva.edu.finances.services.DashboardService;
import com.silva.edu.finances.services.FinanceAccountService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

@Service
public class DashboardServiceImpl implements DashboardService {
    private final FinanceAccountService accounts;
    private final TransactionRepository transactions;
    private final RecurrenceRepository recurrences;
    private final CategoryService categories;
    private final RecurrenceMaterializer materializer;

    public DashboardServiceImpl(FinanceAccountService accounts, TransactionRepository transactions,
                                RecurrenceRepository recurrences, CategoryService categories,
                                RecurrenceMaterializer materializer) {
        this.accounts = accounts;
        this.transactions = transactions;
        this.recurrences = recurrences;
        this.categories = categories;
        this.materializer = materializer;
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
        categories.ensureInvestmentCategory(UserContext.getUser());
        BigDecimal contributions = BigDecimal.ZERO;
        BigDecimal withdrawals = BigDecimal.ZERO;
        for (Object[] row : transactions.sumByClassification(UserContext.getUser(), accountId,
                CategoryClassification.INVESTMENT, new Date())) {
            BigDecimal total = (BigDecimal) row[1];
            if (Boolean.TRUE.equals(row[0])) withdrawals = total;
            else contributions = total;
        }
        return new InvestmentSummaryResponse(accountId, contributions, withdrawals,
                contributions.subtract(withdrawals), account.currency());
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
