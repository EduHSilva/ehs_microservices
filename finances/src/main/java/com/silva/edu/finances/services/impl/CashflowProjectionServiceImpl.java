package com.silva.edu.finances.services.impl;

import com.silva.edu.finances.domain.dtos.response.CashflowProjectionResponse;
import com.silva.edu.finances.domain.entities.CreditCard;
import com.silva.edu.finances.domain.entities.Recurrence;
import com.silva.edu.finances.domain.entities.Transaction;
import com.silva.edu.finances.domain.enums.CardStatus;
import com.silva.edu.finances.domain.enums.RecurrenceStatus;
import com.silva.edu.finances.domain.enums.RecurrenceType;
import com.silva.edu.finances.domain.enums.TransactionEntryType;
import com.silva.edu.finances.domain.enums.TransactionStatus;
import com.silva.edu.finances.repositories.CreditCardRepository;
import com.silva.edu.finances.repositories.RecurrenceRepository;
import com.silva.edu.finances.repositories.TransactionRepository;
import com.silva.edu.finances.services.CashflowProjectionService;
import com.silva.edu.finances.services.FinanceAccountService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;

@Service
public class CashflowProjectionServiceImpl implements CashflowProjectionService {
    private static final ZoneId ZONE = ZoneOffset.UTC;
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private final FinanceAccountService accounts;
    private final TransactionRepository transactions;
    private final RecurrenceRepository recurrences;
    private final CreditCardRepository cards;
    private final CreditCardInvoiceService invoices;

    public CashflowProjectionServiceImpl(FinanceAccountService accounts, TransactionRepository transactions,
                                         RecurrenceRepository recurrences, CreditCardRepository cards,
                                         CreditCardInvoiceService invoices) {
        this.accounts = accounts; this.transactions = transactions; this.recurrences = recurrences;
        this.cards = cards; this.invoices = invoices;
    }

    @Override @Transactional(readOnly = true)
    public CashflowProjectionResponse project(String accountId, int horizonDays) {
        if (!List.of(30, 90, 180).contains(horizonDays))
            throw new IllegalArgumentException("Horizon must be 30, 90 or 180 days");
        var account = accounts.get(accountId);
        UUID userId = accounts.ownerOf(accountId);
        LocalDate today = LocalDate.now(ZONE);
        LocalDate endExclusive = today.plusDays(horizonDays + 1L);
        Date from = Date.from(today.plusDays(1).atStartOfDay(ZONE).toInstant());
        Date to = Date.from(endExclusive.atStartOfDay(ZONE).toInstant());
        Map<LocalDate, DayFlows> flows = new HashMap<>();
        List<CashflowProjectionResponse.Item> items = new ArrayList<>();
        Set<String> scheduledRecurrenceDays = new HashSet<>();

        var futureTransactions = transactions.findAllByAccountIdAndExecutionDateBetween(accountId, from, to,
                PageRequest.of(0, 5000, Sort.by("executionDate").ascending())).getContent();
        for (Transaction transaction : futureTransactions) {
            if (transaction.getStatus() != TransactionStatus.OK || transaction.getExecutionDate() == null) continue;
            // Compra no cartão não sai do caixa na data da compra: entra pela fatura, abaixo.
            if (transaction.getEntryType() == TransactionEntryType.CARD) continue;
            LocalDate date = transaction.getExecutionDate().toInstant().atZone(ZONE).toLocalDate();
            if (transaction.getRecurrence() != null)
                scheduledRecurrenceDays.add(transaction.getRecurrence().getId() + "|" + YearMonth.from(date));
            addKnown(flows, items, date, transaction.getTitle(), transaction.getValue(), transaction.isIncome(), "TRANSACTION");
        }

        for (Recurrence recurrence : recurrences.findAllByAccountIdAndStatus(accountId, RecurrenceStatus.ACTIVE)) {
            if (recurrence.getAccountId() == null || !accountId.equals(recurrence.getAccountId())) continue;
            // Ocorrências de cartão também saem pela fatura projetada, não pelo dia da recorrência.
            boolean onCard = recurrence.getCreditCard() != null;
            if (recurrence.getType() == RecurrenceType.MONTH) {
                for (int offset = 0; offset <= horizonDays / 28 + 2; offset++) {
                    LocalDate date = scheduledDate(today, offset, recurrence.getDay());
                    if (date.isAfter(today) && date.isBefore(endExclusive) && !onCard
                            && !scheduledRecurrenceDays.contains(recurrence.getId() + "|" + YearMonth.from(date)))
                        addKnown(flows, items, date, recurrence.getTitle(), recurrence.getValue(), recurrence.isIncome(), "RECURRENCE");
                }
            } else if (recurrence.getType() == RecurrenceType.INSTALLMENT && recurrence.getInstallments() != null) {
                int remaining = Math.max(0, recurrence.getInstallments() - Optional.ofNullable(recurrence.getPayed()).orElse(0));
                int created = 0;
                for (int offset = 0; created < remaining && offset <= horizonDays / 28 + remaining + 2; offset++) {
                    LocalDate date = scheduledDate(today, offset, recurrence.getDay());
                    if (!date.isAfter(today)) continue;
                    if (date.isBefore(endExclusive) && !onCard
                            && !scheduledRecurrenceDays.contains(recurrence.getId() + "|" + YearMonth.from(date)))
                        addKnown(flows, items, date, recurrence.getTitle(), recurrence.getValue(), recurrence.isIncome(), "INSTALLMENT");
                    created++;
                }
            }
        }

        // Uma saída por ciclo de cartão com saldo em aberto, no vencimento (atrasada cai amanhã).
        LocalDate firstClosing = YearMonth.from(today).minusMonths(CreditCardInvoiceCalculator.LOOKBACK_MONTHS).atDay(1);
        for (CreditCard card : cards.findAllByAccountIdAndStatus(accountId, CardStatus.ACTIVE)) {
            CreditCardInvoiceService.Ledger ledger = invoices.ledger(card, userId, today.plusDays(horizonDays));
            for (YearMonth month : ledger.months()) {
                CreditCardInvoiceService.Invoice invoice = invoices.invoice(card, ledger, month);
                LocalDate closing = invoice.closingDate();
                if (closing.isBefore(firstClosing)) continue;
                if (invoice.dueDate().isAfter(today.plusDays(horizonDays))) continue;
                if (invoice.remaining().signum() <= 0) continue;
                LocalDate date = invoice.dueDate().isAfter(today) ? invoice.dueDate() : today.plusDays(1);
                addKnown(flows, items, date, "Fatura " + card.getName() + " " + month, invoice.remaining(), false, "INVOICE");
            }
        }

        LocalDate historyStart = YearMonth.from(today).minusMonths(6).atDay(1);
        Date historyFrom = Date.from(historyStart.atStartOfDay(ZONE).toInstant());
        Date historyTo = Date.from(YearMonth.from(today).atDay(1).atStartOfDay(ZONE).toInstant());
        BigDecimal variableIncome = ZERO, variableExpenses = ZERO;
        long sampleCount = 0;
        for (Object[] row : transactions.sumVariableHistory(userId, accountId, historyFrom, historyTo)) {
            BigDecimal total = (BigDecimal) row[1];
            sampleCount += ((Number) row[2]).longValue();
            if (Boolean.TRUE.equals(row[0])) variableIncome = total;
            else variableExpenses = total;
        }
        BigDecimal averageIncome = variableIncome.divide(BigDecimal.valueOf(6), 4, RoundingMode.HALF_UP);
        BigDecimal averageExpenses = variableExpenses.divide(BigDecimal.valueOf(6), 4, RoundingMode.HALF_UP);

        BigDecimal balance = account.balance();
        List<CashflowProjectionResponse.Point> series = new ArrayList<>();
        series.add(new CashflowProjectionResponse.Point(today, balance, ZERO, ZERO, ZERO, ZERO));
        for (int day = 1; day <= horizonDays; day++) {
            LocalDate date = today.plusDays(day);
            DayFlows flow = flows.getOrDefault(date, new DayFlows());
            BigDecimal estimatedIncome = averageIncome.divide(BigDecimal.valueOf(date.lengthOfMonth()), 4, RoundingMode.HALF_UP);
            BigDecimal estimatedExpenses = averageExpenses.divide(BigDecimal.valueOf(date.lengthOfMonth()), 4, RoundingMode.HALF_UP);
            balance = balance.add(flow.income).subtract(flow.expenses).add(estimatedIncome).subtract(estimatedExpenses);
            series.add(new CashflowProjectionResponse.Point(date, balance, flow.income, flow.expenses,
                    estimatedIncome, estimatedExpenses));
        }
        items.sort(Comparator.comparing(CashflowProjectionResponse.Item::date)
                .thenComparing(CashflowProjectionResponse.Item::title, String.CASE_INSENSITIVE_ORDER));
        String confidence = sampleCount >= 24 ? "HIGH" : sampleCount >= 6 ? "MEDIUM" : "LOW";
        return new CashflowProjectionResponse(accountId, horizonDays, Instant.now(), account.balance(), balance,
                confidence, List.of("Saldo atual = saldo de abertura + transações confirmadas até hoje.",
                "Recorrências mensais e parcelas ativas são projetadas nas datas cadastradas (exceto as de cartão).",
                "Compras de cartão não saem na data da compra: cada fatura em aberto entra como uma saída no vencimento (atrasada, amanhã).",
                "Transferências entram na própria data e a média variável ignora transferências e pagamentos de fatura.",
                "Fluxo variável usa médias de seis meses completos, excluindo transações vinculadas a recorrências.",
                "Estimativas são distribuídas diariamente; não representam saldo realizado."), series, items);
    }

    private void addKnown(Map<LocalDate, DayFlows> flows, List<CashflowProjectionResponse.Item> items,
                          LocalDate date, String title, BigDecimal amount, boolean income, String kind) {
        DayFlows day = flows.computeIfAbsent(date, ignored -> new DayFlows());
        if (income) day.income = day.income.add(amount); else day.expenses = day.expenses.add(amount);
        items.add(new CashflowProjectionResponse.Item(date, title, amount, income, kind));
    }
    private LocalDate scheduledDate(LocalDate today, int monthOffset, int day) {
        YearMonth month = YearMonth.from(today).plusMonths(monthOffset);
        return month.atDay(Math.min(day, month.lengthOfMonth()));
    }
    private static class DayFlows { BigDecimal income = ZERO; BigDecimal expenses = ZERO; }
}
