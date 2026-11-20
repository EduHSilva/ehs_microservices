package com.silva.edu.finances.services.impl;

import com.silva.edu.finances.domain.entities.CreditCard;
import com.silva.edu.finances.domain.entities.Transaction;
import com.silva.edu.finances.domain.enums.InvoiceStatus;
import com.silva.edu.finances.domain.enums.TransactionStatus;
import com.silva.edu.finances.repositories.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/**
 * Monta as faturas de um cartão a partir das compras CARD OK e dos pagamentos INVOICE_PAYMENT OK.
 * A janela de compras é calculada sem `status = OK` e filtrada em memória: o {@code @SQLRestriction}
 * de DELETED junto com o filtro de status do Hibernate pode ocultar linhas já carregadas na sessão.
 * Todas as consultas recebem o userId para isolar pelo dono da conta.
 */
@Service
public class CreditCardInvoiceService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final int LOOKBACK_MONTHS = CreditCardInvoiceCalculator.LOOKBACK_MONTHS;

    private final TransactionRepository transactions;

    public CreditCardInvoiceService(TransactionRepository transactions) {
        this.transactions = transactions;
    }

    /** Fatura de um ciclo: compras CARD OK (total) e pagamentos OK (paid). */
    public record Invoice(CreditCard card, YearMonth invoiceMonth, LocalDate closingDate, LocalDate dueDate,
                          BigDecimal total, BigDecimal paid) {
        public BigDecimal remaining() {
            return CreditCardInvoiceCalculator.remaining(total, paid);
        }

        public InvoiceStatus status() {
            return CreditCardInvoiceCalculator.status(total, paid);
        }

        public boolean hasOpenBalance() {
            return remaining().signum() > 0;
        }
    }

    /** Compras e pagamentos agregados por mês de vencimento da fatura. */
    public static final class Ledger {
        private final Map<YearMonth, BigDecimal> purchased = new HashMap<>();
        private final Map<YearMonth, BigDecimal> paid = new HashMap<>();

        public BigDecimal total(YearMonth month) {
            return purchased.getOrDefault(month, ZERO);
        }

        public BigDecimal paid(YearMonth month) {
            return paid.getOrDefault(month, ZERO);
        }

        /** Ciclos com compras ou pagamentos. */
        public Set<YearMonth> months() {
            Set<YearMonth> months = new TreeSet<>(purchased.keySet());
            months.addAll(paid.keySet());
            return months;
        }
    }

    /** Ledger do cartão considerando compras com data &lt;= {@code purchasesUntil}. */
    public Ledger ledger(CreditCard card, UUID userId, LocalDate purchasesUntil) {
        Ledger ledger = new Ledger();
        LocalDate from = YearMonth.from(purchasesUntil).minusMonths(LOOKBACK_MONTHS).atDay(1);
        for (Transaction purchase : transactions.findCardPurchasesBefore(card.getAccountId(),
                CreditCardInvoiceCalculator.toDate(purchasesUntil.plusDays(1)))) {
            if (!Objects.equals(userId, purchase.getUserId()) || purchase.getExecutionDate() == null) continue;
            if (purchase.getStatus() != TransactionStatus.OK || purchase.getValue() == null) continue;
            if (!card.getId().equals(cardIdOf(purchase))) continue;
            LocalDate date = CreditCardInvoiceCalculator.toLocalDate(purchase.getExecutionDate());
            if (date.isBefore(from)) continue;
            YearMonth invoiceMonth = CreditCardInvoiceCalculator.cycleOf(date, card.getClosingDay(), card.getDueDay())
                    .invoiceMonth();
            ledger.purchased.merge(invoiceMonth, purchase.getValue(), BigDecimal::add);
        }
        for (Transaction payment : transactions.findInvoicePaymentsByCard(card.getId())) {
            if (!Objects.equals(userId, payment.getUserId()) || payment.getInvoiceDueDate() == null) continue;
            if (payment.getStatus() != TransactionStatus.OK || payment.getValue() == null) continue;
            LocalDate due = CreditCardInvoiceCalculator.toLocalDate(payment.getInvoiceDueDate());
            ledger.paid.merge(YearMonth.from(due), payment.getValue(), BigDecimal::add);
        }
        return ledger;
    }

    /** Pagamentos ativos (OK) de um ciclo, do mais antigo para o mais recente. */
    public List<Transaction> payments(CreditCard card, UUID userId, YearMonth invoiceMonth) {
        List<Transaction> result = new ArrayList<>();
        for (Transaction payment : transactions.findInvoicePaymentsByCard(card.getId())) {
            if (!Objects.equals(userId, payment.getUserId()) || payment.getInvoiceDueDate() == null
                    || payment.getExecutionDate() == null) continue;
            if (payment.getStatus() != TransactionStatus.OK || payment.getValue() == null) continue;
            LocalDate due = CreditCardInvoiceCalculator.toLocalDate(payment.getInvoiceDueDate());
            if (YearMonth.from(due).equals(invoiceMonth)) result.add(payment);
        }
        result.sort(Comparator.comparing(Transaction::getExecutionDate));
        return result;
    }

    public Invoice invoice(CreditCard card, Ledger ledger, YearMonth invoiceMonth) {
        CreditCardInvoiceCalculator.Cycle cycle = CreditCardInvoiceCalculator.cycleOfInvoiceMonth(
                invoiceMonth, card.getClosingDay(), card.getDueDay());
        return new Invoice(card, invoiceMonth, cycle.closingDate(), cycle.dueDate(),
                ledger.total(invoiceMonth), ledger.paid(invoiceMonth));
    }

    /** Todas as faturas (ciclos com compras ou pagamentos), da mais recente para a mais antiga. */
    public List<Invoice> invoices(CreditCard card, UUID userId, LocalDate purchasesUntil) {
        Ledger ledger = ledger(card, userId, purchasesUntil);
        List<Invoice> result = new ArrayList<>();
        for (YearMonth month : ledger.months()) result.add(invoice(card, ledger, month));
        result.sort(Comparator.comparing(Invoice::dueDate).reversed());
        return result;
    }

    /** Soma do restante das faturas com saldo em aberto. */
    public BigDecimal openTotal(CreditCard card, UUID userId, LocalDate purchasesUntil) {
        BigDecimal total = ZERO;
        for (Invoice invoice : invoices(card, userId, purchasesUntil)) {
            if (invoice.hasOpenBalance()) total = total.add(invoice.remaining());
        }
        return total;
    }

    /** Ciclos do cartão que já têm compras até hoje e seguem com saldo em aberto. */
    public List<Invoice> openInvoices(CreditCard card, UUID userId, LocalDate today) {
        List<Invoice> result = new ArrayList<>();
        for (Invoice invoice : invoices(card, userId, today)) if (invoice.hasOpenBalance()) result.add(invoice);
        return result;
    }

    static UUID cardIdOf(Transaction transaction) {
        return transaction.getCreditCard() == null ? null : transaction.getCreditCard().getId();
    }
}
