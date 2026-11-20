package com.silva.edu.finances.services.impl;

import com.silva.edu.finances.domain.enums.InvoiceStatus;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;

/**
 * Cálculo puro de ciclo/fatura de cartão (sem banco), para ser testado isoladamente.
 *
 * <p>Ciclo: fecha no {@code closingDay} de cada mês (ajustado ao último dia em meses curtos) e
 * recebe as compras com data até o fechamento — compra no próprio dia entra nele. O vencimento é
 * o primeiro {@code dueDay} (ajustado) estritamente após o fechamento, procurando no máximo um
 * mês à frente. A fatura é identificada pelo mês do vencimento ({@code AAAA-MM}).</p>
 */
public final class CreditCardInvoiceCalculator {
    /** Janela usada para carregar compras: nenhum ciclo usado pelos endpoints começa antes disto. */
    public static final int LOOKBACK_MONTHS = 36;

    private static final ZoneId ZONE = ZoneOffset.UTC;

    private CreditCardInvoiceCalculator() { }

    public record Cycle(YearMonth invoiceMonth, java.time.LocalDate closingDate, java.time.LocalDate dueDate,
                        java.time.LocalDate previousClosingDate) { }

    public static java.time.LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZONE).toLocalDate();
    }

    public static Date toDate(java.time.LocalDate date) {
        return Date.from(date.atStartOfDay(ZONE).toInstant());
    }

    public static java.time.LocalDate closingDate(YearMonth month, int closingDay) {
        return month.atDay(clampDay(closingDay, month));
    }

    public static java.time.LocalDate dueDate(java.time.LocalDate closing, int dueDay) {
        YearMonth month = YearMonth.from(closing);
        // Mesmo mês só quando o dueDay fica estritamente depois do fechamento; senão, mês seguinte.
        java.time.LocalDate sameMonth = month.atDay(clampDay(dueDay, month));
        if (sameMonth.isAfter(closing)) return sameMonth;
        YearMonth next = month.plusMonths(1);
        return next.atDay(clampDay(dueDay, next));
    }

    /** Ciclo (fatura) ao qual pertence uma compra feita em {@code purchaseDate}. */
    public static Cycle cycleOf(java.time.LocalDate purchaseDate, int closingDay, int dueDay) {
        YearMonth month = YearMonth.from(purchaseDate);
        java.time.LocalDate closing = closingDate(month, closingDay);
        if (purchaseDate.isAfter(closing)) {
            month = month.plusMonths(1);
            closing = closingDate(month, closingDay);
        }
        java.time.LocalDate due = dueDate(closing, dueDay);
        return new Cycle(YearMonth.from(due), closing, due, closingDate(month.minusMonths(1), closingDay));
    }

    /**
     * Ciclo cuja fatura é identificada por {@code invoiceMonth}: o fechamento ocorre no mês anterior
     * quando o vencimento leva mais de um mês para chegar (dueDay &lt;= closingDay) ou no próprio mês
     * da fatura quando o vencimento cai antes do fechamento seguinte (dueDay &gt; closingDay).
     */
    public static Cycle cycleOfInvoiceMonth(YearMonth invoiceMonth, int closingDay, int dueDay) {
        YearMonth closingMonth = invoiceMonth.minusMonths(1);
        if (!YearMonth.from(dueDate(closingDate(closingMonth, closingDay), dueDay)).equals(invoiceMonth))
            closingMonth = invoiceMonth;
        java.time.LocalDate closing = closingDate(closingMonth, closingDay);
        return new Cycle(invoiceMonth, closing, dueDate(closing, dueDay),
                closingDate(closingMonth.minusMonths(1), closingDay));
    }

    public static InvoiceStatus status(BigDecimal total, BigDecimal paid) {
        BigDecimal t = total == null ? BigDecimal.ZERO : total;
        BigDecimal p = paid == null ? BigDecimal.ZERO : paid;
        if (p.signum() <= 0) return InvoiceStatus.OPEN;
        return p.compareTo(t) >= 0 ? InvoiceStatus.PAID : InvoiceStatus.PARTIAL;
    }

    public static BigDecimal remaining(BigDecimal total, BigDecimal paid) {
        BigDecimal t = total == null ? BigDecimal.ZERO : total;
        BigDecimal p = paid == null ? BigDecimal.ZERO : paid;
        BigDecimal remaining = t.subtract(p);
        return remaining.signum() < 0 ? BigDecimal.ZERO : remaining;
    }

    private static int clampDay(int day, YearMonth month) {
        return Math.max(1, Math.min(day, month.lengthOfMonth()));
    }
}
