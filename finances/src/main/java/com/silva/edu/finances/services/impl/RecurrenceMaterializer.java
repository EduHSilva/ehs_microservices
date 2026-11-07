package com.silva.edu.finances.services.impl;

import com.silva.edu.finances.domain.entities.Recurrence;
import com.silva.edu.finances.domain.entities.Transaction;
import com.silva.edu.finances.domain.enums.RecurrenceStatus;
import com.silva.edu.finances.domain.enums.RecurrenceType;
import com.silva.edu.finances.domain.enums.TransactionStatus;
import com.silva.edu.finances.repositories.RecurrenceRepository;
import com.silva.edu.finances.repositories.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Materializes due recurrence occurrences as PENDING transactions so the user can confirm them
 * from the dashboard. Generation is idempotent per recurrence/month and capped by the installment
 * remaining count, so repeated requests never duplicate a due occurrence.
 */
@Service
@Transactional
public class RecurrenceMaterializer {
    private static final int MAX_LOOKBACK_MONTHS = 12;
    private static final java.time.ZoneId ZONE = ZoneOffset.UTC;

    private final RecurrenceRepository recurrences;
    private final TransactionRepository transactions;

    public RecurrenceMaterializer(RecurrenceRepository recurrences, TransactionRepository transactions) {
        this.recurrences = recurrences;
        this.transactions = transactions;
    }

    public void materialize(String accountId) {
        LocalDate today = LocalDate.now(ZONE);
        YearMonth currentMonth = YearMonth.from(today);
        YearMonth firstWindow = currentMonth.minusMonths(MAX_LOOKBACK_MONTHS);
        LocalDate windowStart = firstWindow.atDay(1);

        // Lock active recurrences first so concurrent dashboard/list requests cannot materialize
        // the same occurrence twice.
        var activeRecurrences = recurrences.findAllForUpdate(accountId, RecurrenceStatus.ACTIVE);
        Map<UUID, Set<YearMonth>> existingMonths = new HashMap<>();
        Map<UUID, Long> pendingCounts = new HashMap<>();
        for (Transaction transaction : transactions.findAllRecurrenceLinkedByAccountId(
                accountId, toDate(windowStart), toDate(today.plusDays(1)))) {
            if (transaction.getRecurrence() == null || transaction.getExecutionDate() == null) continue;
            UUID recurrenceId = transaction.getRecurrence().getId();
            existingMonths.computeIfAbsent(recurrenceId, ignored -> new HashSet<>())
                    .add(YearMonth.from(toLocalDate(transaction.getExecutionDate())));
            if (transaction.getStatus() == TransactionStatus.PENDING) {
                pendingCounts.merge(recurrenceId, 1L, Long::sum);
            }
        }

        for (Recurrence recurrence : activeRecurrences) {
            if (!accountId.equals(recurrence.getAccountId())) continue;
            LocalDate created = recurrence.getCreatedDate() == null ? today : toLocalDate(recurrence.getCreatedDate());
            YearMonth start = YearMonth.from(created.isAfter(windowStart) ? created : windowStart);
            Set<YearMonth> existing = existingMonths.computeIfAbsent(recurrence.getId(), ignored -> new HashSet<>());
            long pendingCount = pendingCounts.getOrDefault(recurrence.getId(), 0L);
            long remaining = remainingInstallments(recurrence);

            for (YearMonth month = start; !month.isAfter(currentMonth); month = month.plusMonths(1)) {
                LocalDate due = month.atDay(Math.min(recurrence.getDay(), month.lengthOfMonth()));
                if (due.isBefore(created) || due.isAfter(today) || existing.contains(month)) continue;
                if (pendingCount >= remaining) break;
                transactions.save(pendingTransaction(recurrence, due));
                existing.add(month);
                pendingCount++;
            }
        }
    }

    private long remainingInstallments(Recurrence recurrence) {
        if (recurrence.getType() != RecurrenceType.INSTALLMENT) return Long.MAX_VALUE;
        int total = recurrence.getInstallments() == null ? 0 : recurrence.getInstallments();
        int payed = recurrence.getPayed() == null ? 0 : recurrence.getPayed();
        return Math.max(0, total - payed);
    }

    private Transaction pendingTransaction(Recurrence recurrence, LocalDate due) {
        Transaction transaction = new Transaction();
        transaction.setUserId(recurrence.getUserId());
        transaction.setAccountId(recurrence.getAccountId());
        transaction.setTitle(recurrence.getTitle());
        transaction.setValue(recurrence.getValue());
        transaction.setIncome(recurrence.isIncome());
        transaction.setCategory(recurrence.getCategory());
        transaction.setRecurrence(recurrence);
        transaction.setExecutionDate(toDate(due));
        transaction.setStatus(TransactionStatus.PENDING);
        return transaction;
    }

    private static Date toDate(LocalDate date) {
        return Date.from(date.atStartOfDay(ZONE).toInstant());
    }

    private static LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZONE).toLocalDate();
    }
}
