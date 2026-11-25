package com.silva.edu.finances.services.impl;

import com.silva.edu.finances.domain.enums.InvoiceStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

class CreditCardInvoiceCalculatorTest {

    @Test
    void purchaseOnClosingDayBelongsToMonthClosing() {
        // Fecha dia 10: compra no dia 10 entra no ciclo que fecha em 10/03, vence em 20/03.
        CreditCardInvoiceCalculator.Cycle cycle = CreditCardInvoiceCalculator.cycleOf(
                LocalDate.of(2026, 3, 10), 10, 20);

        assertThat(cycle.invoiceMonth()).isEqualTo(YearMonth.of(2026, 3));
        assertThat(cycle.closingDate()).isEqualTo(LocalDate.of(2026, 3, 10));
        assertThat(cycle.dueDate()).isEqualTo(LocalDate.of(2026, 3, 20));
    }

    @Test
    void purchaseAfterClosingDayGoesToNextCycle() {
        CreditCardInvoiceCalculator.Cycle cycle = CreditCardInvoiceCalculator.cycleOf(
                LocalDate.of(2026, 3, 11), 10, 20);

        assertThat(cycle.invoiceMonth()).isEqualTo(YearMonth.of(2026, 4));
        assertThat(cycle.closingDate()).isEqualTo(LocalDate.of(2026, 4, 10));
        assertThat(cycle.previousClosingDate()).isEqualTo(LocalDate.of(2026, 3, 10));
    }

    @Test
    void dueDayBeforeClosingDayMovesDueDateToNextMonth() {
        // Fecha dia 25, vence dia 5: o vencimento é o primeiro dia 5 estritamente após 25/03.
        CreditCardInvoiceCalculator.Cycle cycle = CreditCardInvoiceCalculator.cycleOf(
                LocalDate.of(2026, 3, 20), 25, 5);

        assertThat(cycle.closingDate()).isEqualTo(LocalDate.of(2026, 3, 25));
        assertThat(cycle.dueDate()).isEqualTo(LocalDate.of(2026, 4, 5));
        assertThat(cycle.invoiceMonth()).isEqualTo(YearMonth.of(2026, 4));
    }

    @Test
    void dueDayAfterClosingDayStaysInSameMonth() {
        CreditCardInvoiceCalculator.Cycle cycle = CreditCardInvoiceCalculator.cycleOf(
                LocalDate.of(2026, 3, 1), 10, 20);

        assertThat(cycle.dueDate()).isEqualTo(LocalDate.of(2026, 3, 20));
        assertThat(cycle.invoiceMonth()).isEqualTo(YearMonth.of(2026, 3));
    }

    @Test
    void dueDateMustBeStrictlyAfterClosing() {
        // Mesmo dia: não pode vencer no próprio fechamento, então cai no mês seguinte.
        LocalDate closing = CreditCardInvoiceCalculator.closingDate(YearMonth.of(2026, 3), 10);
        assertThat(CreditCardInvoiceCalculator.dueDate(closing, 10, 10)).isEqualTo(LocalDate.of(2026, 4, 10));
    }

    @Test
    void shortMonthsClampClosingAndDueDays() {
        // 31 em fevereiro vira 28; vencimento no mesmo mês, dia 31 também ajustado.
        assertThat(CreditCardInvoiceCalculator.closingDate(YearMonth.of(2026, 2), 31))
                .isEqualTo(LocalDate.of(2026, 2, 28));
        assertThat(CreditCardInvoiceCalculator.closingDate(YearMonth.of(2026, 4), 31))
                .isEqualTo(LocalDate.of(2026, 4, 30));
        // Fecha 28/02 e vence 31 (ajustado a 28/02) — não pode ser depois, então 31/03.
        assertThat(CreditCardInvoiceCalculator.dueDate(LocalDate.of(2026, 2, 28), 31, 31))
                .isEqualTo(LocalDate.of(2026, 3, 31));
    }

    @Test
    void februaryDueDayAfterClosingDoesNotCollideWithNextCycle() {
        // Fecha 28 e vence 29: em fevereiro o dia 29 não existe. Sem o deslocamento, o ciclo de
        // fev e o de mar venceriam em 29/03 e dividiriam a fatura 2026-03.
        assertThat(CreditCardInvoiceCalculator.dueDate(LocalDate.of(2026, 2, 28), 28, 29))
                .isEqualTo(LocalDate.of(2026, 2, 28));
        assertThat(CreditCardInvoiceCalculator.dueDate(LocalDate.of(2026, 3, 28), 28, 29))
                .isEqualTo(LocalDate.of(2026, 3, 29));

        assertThat(CreditCardInvoiceCalculator.cycleOf(LocalDate.of(2026, 2, 20), 28, 29).invoiceMonth())
                .isEqualTo(YearMonth.of(2026, 2));
        assertThat(CreditCardInvoiceCalculator.cycleOf(LocalDate.of(2026, 3, 20), 28, 29).invoiceMonth())
                .isEqualTo(YearMonth.of(2026, 3));
        assertThat(CreditCardInvoiceCalculator.cycleOfInvoiceMonth(YearMonth.of(2026, 2), 28, 29).dueDate())
                .isEqualTo(LocalDate.of(2026, 2, 28));
    }

    @Test
    void shortMonthClosingAtMonthEndShiftsDueToAvoidInvoiceCollision() {
        // Fecha 30 e vence 31: abril não tem dia 31, então o ciclo de abril fica com o vencimento
        // no dia 30/04 e o de maio em 31/05.
        assertThat(CreditCardInvoiceCalculator.dueDate(LocalDate.of(2026, 4, 30), 30, 31))
                .isEqualTo(LocalDate.of(2026, 4, 30));
        assertThat(CreditCardInvoiceCalculator.dueDate(LocalDate.of(2026, 5, 30), 30, 31))
                .isEqualTo(LocalDate.of(2026, 5, 31));
    }

    @Test
    void purchaseInShortMonthUsesClampedClosing() {
        CreditCardInvoiceCalculator.Cycle cycle = CreditCardInvoiceCalculator.cycleOf(
                LocalDate.of(2026, 2, 28), 30, 10);

        assertThat(cycle.closingDate()).isEqualTo(LocalDate.of(2026, 2, 28));
        assertThat(cycle.dueDate()).isEqualTo(LocalDate.of(2026, 3, 10));
        assertThat(cycle.invoiceMonth()).isEqualTo(YearMonth.of(2026, 3));
    }

    @Test
    void cycleOfInvoiceMonthReversesTheMapping() {
        CreditCardInvoiceCalculator.Cycle late = CreditCardInvoiceCalculator.cycleOfInvoiceMonth(
                YearMonth.of(2026, 4), 10, 20);
        assertThat(late.closingDate()).isEqualTo(LocalDate.of(2026, 4, 10));
        assertThat(late.dueDate()).isEqualTo(LocalDate.of(2026, 4, 20));

        CreditCardInvoiceCalculator.Cycle early = CreditCardInvoiceCalculator.cycleOfInvoiceMonth(
                YearMonth.of(2026, 4), 25, 5);
        assertThat(early.closingDate()).isEqualTo(LocalDate.of(2026, 3, 25));
        assertThat(early.dueDate()).isEqualTo(LocalDate.of(2026, 4, 5));
        assertThat(early.previousClosingDate()).isEqualTo(LocalDate.of(2026, 2, 25));
    }

    @Test
    void invoiceStatusAndRemaining() {
        assertThat(CreditCardInvoiceCalculator.status(BigDecimal.TEN, BigDecimal.ZERO)).isEqualTo(InvoiceStatus.OPEN);
        assertThat(CreditCardInvoiceCalculator.status(BigDecimal.TEN, BigDecimal.ONE)).isEqualTo(InvoiceStatus.PARTIAL);
        assertThat(CreditCardInvoiceCalculator.status(BigDecimal.TEN, BigDecimal.TEN)).isEqualTo(InvoiceStatus.PAID);
        assertThat(CreditCardInvoiceCalculator.remaining(BigDecimal.TEN, BigDecimal.valueOf(4)))
                .isEqualByComparingTo(BigDecimal.valueOf(6));
        assertThat(CreditCardInvoiceCalculator.remaining(BigDecimal.TEN, BigDecimal.valueOf(11)).signum()).isZero();
    }
}
