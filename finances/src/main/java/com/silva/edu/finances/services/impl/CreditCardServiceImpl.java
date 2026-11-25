package com.silva.edu.finances.services.impl;

import com.silva.edu.finances.domain.dtos.request.AddCreditCardRequest;
import com.silva.edu.finances.domain.dtos.request.InvoicePaymentRequest;
import com.silva.edu.finances.domain.dtos.request.UpdateCreditCardRequest;
import com.silva.edu.finances.domain.dtos.response.CreditCardInvoicePaymentResponse;
import com.silva.edu.finances.domain.dtos.response.CreditCardInvoiceResponse;
import com.silva.edu.finances.domain.dtos.response.CreditCardResponse;
import com.silva.edu.finances.domain.entities.CreditCard;
import com.silva.edu.finances.domain.entities.Transaction;
import com.silva.edu.finances.domain.enums.CardStatus;
import com.silva.edu.finances.domain.enums.RecurrenceStatus;
import com.silva.edu.finances.domain.enums.TransactionEntryType;
import com.silva.edu.finances.domain.enums.TransactionStatus;
import com.silva.edu.finances.infra.exceptions.CustomExceptions;
import com.silva.edu.finances.repositories.CreditCardRepository;
import com.silva.edu.finances.repositories.RecurrenceRepository;
import com.silva.edu.finances.repositories.TransactionRepository;
import com.silva.edu.finances.services.CreditCardService;
import com.silva.edu.finances.services.FinanceAccountService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CreditCardServiceImpl implements CreditCardService {
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final CreditCardRepository cards;
    private final TransactionRepository transactions;
    private final RecurrenceRepository recurrences;
    private final FinanceAccountService accounts;
    private final CreditCardInvoiceService invoices;

    public CreditCardServiceImpl(CreditCardRepository cards, TransactionRepository transactions,
                                 RecurrenceRepository recurrences, FinanceAccountService accounts,
                                 CreditCardInvoiceService invoices) {
        this.cards = cards;
        this.transactions = transactions;
        this.recurrences = recurrences;
        this.accounts = accounts;
        this.invoices = invoices;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CreditCardResponse> list(String accountId) {
        accounts.requireAccess(accountId);
        UUID owner = accounts.ownerOf(accountId);
        LocalDate today = today();
        List<CreditCardResponse> result = new ArrayList<>();
        for (CreditCard card : cards.findAllByAccountIdAndStatus(accountId, CardStatus.ACTIVE)) {
            result.add(response(card, owner, today));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public CreditCardResponse get(UUID cardId) {
        CreditCard card = requireCard(cardId);
        return response(card, accounts.ownerOf(card.getAccountId()), today());
    }

    @Override
    public CreditCardResponse create(AddCreditCardRequest request) {
        if (request.accountId() == null || request.accountId().isBlank())
            throw new IllegalArgumentException("accountId is required");
        String accountId = request.accountId().trim();
        accounts.requireAccess(accountId);
        validate(request.name(), request.cardLimit(), request.closingDay(), request.dueDay());
        CreditCard card = new CreditCard();
        card.setUserId(accounts.ownerOf(accountId));
        card.setAccountId(accountId);
        card.setName(request.name().trim());
        card.setCardLimit(request.cardLimit());
        card.setClosingDay(request.closingDay());
        card.setDueDay(request.dueDay());
        card.setStatus(CardStatus.ACTIVE);
        cards.save(card);
        if (request.initialBalance() != null && request.initialBalance().signum() > 0)
            recordInitialBalance(card, request.initialBalance());
        return response(card, card.getUserId(), today());
    }

    /** Saldo inicial vira compra CARD confirmada com a data de hoje, que cai no ciclo em aberto. */
    private void recordInitialBalance(CreditCard card, BigDecimal value) {
        Transaction purchase = new Transaction();
        purchase.setUserId(card.getUserId());
        purchase.setAccountId(card.getAccountId());
        purchase.setTitle("Saldo inicial do cartão");
        purchase.setValue(value);
        purchase.setIncome(false);
        purchase.setCategory(null);
        purchase.setCreditCard(card);
        purchase.setEntryType(TransactionEntryType.CARD);
        purchase.setExecutionDate(CreditCardInvoiceCalculator.toDate(today()));
        purchase.setStatus(TransactionStatus.OK);
        transactions.save(purchase);
    }

    @Override
    public CreditCardResponse update(UUID cardId, UpdateCreditCardRequest request) {
        CreditCard card = requireCard(cardId);
        validate(request.name(), request.cardLimit(), request.closingDay(), request.dueDay());
        boolean cycleChanged = card.getClosingDay() != request.closingDay() || card.getDueDay() != request.dueDay();
        // Ciclos históricos não são recalculados: com pagamento registrado não se mexe no ciclo.
        if (cycleChanged && transactions.countActiveInvoicePayments(cardId) > 0)
            throw new IllegalArgumentException("Cannot change closing/due day of a card that already has invoice payments");
        card.setName(request.name().trim());
        card.setCardLimit(request.cardLimit());
        card.setClosingDay(request.closingDay());
        card.setDueDay(request.dueDay());
        cards.save(card);
        return response(card, accounts.ownerOf(card.getAccountId()), today());
    }

    @Override
    public void delete(UUID cardId) {
        CreditCard card = requireCard(cardId);
        // Recorrência ativa continuaria gerando ocorrências sem fatura pagável após o cartão sair.
        if (recurrences.existsByCreditCardIdAndStatus(cardId, RecurrenceStatus.ACTIVE))
            throw new CustomExceptions.ConflictException(
                    "This card is linked to active recurrences. Remove or unlink them before deleting the card");
        UUID owner = accounts.ownerOf(card.getAccountId());
        // Compras antigas permanecem como histórico; só bloqueia fatura com saldo em aberto.
        for (CreditCardInvoiceService.Invoice invoice : invoices.invoices(card, owner, today())) {
            if (invoice.hasOpenBalance())
                throw new CustomExceptions.ConflictException("Cannot delete a card with open invoices ("
                        + invoice.invoiceMonth() + ")");
        }
        cards.delete(card);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CreditCardInvoiceResponse> invoices(UUID cardId) {
        CreditCard card = requireCard(cardId);
        UUID owner = accounts.ownerOf(card.getAccountId());
        List<CreditCardInvoiceResponse> result = new ArrayList<>();
        for (CreditCardInvoiceService.Invoice invoice : invoices.invoices(card, owner, today())) {
            result.add(toResponse(invoice, invoices.payments(card, owner, invoice.invoiceMonth())));
        }
        return result;
    }

    @Override
    public CreditCardInvoiceResponse payInvoice(UUID cardId, String invoiceMonth, InvoicePaymentRequest request) {
        CreditCard card = requireCard(cardId);
        YearMonth month = parseMonth(invoiceMonth);
        String accountId = card.getAccountId();
        UUID owner = accounts.ownerOf(accountId);
        if (request.value() == null || request.value().signum() <= 0)
            throw new IllegalArgumentException("Payment value must be greater than zero");
        if (request.date() == null)
            throw new IllegalArgumentException("Payment date is required");

        CreditCardInvoiceService.Invoice invoice = invoices.invoice(card, invoices.ledger(card, owner, request.date()), month);
        if (hasPendingPurchase(card, month))
            throw new IllegalArgumentException("Cannot pay an invoice with pending card purchases in its cycle");
        BigDecimal remaining = invoice.remaining();
        if (remaining.signum() <= 0)
            throw new IllegalArgumentException("Invoice has no open balance");
        if (request.value().compareTo(remaining) > 0)
            throw new IllegalArgumentException("Payment value exceeds the invoice remaining amount");

        Transaction payment = new Transaction();
        payment.setUserId(owner);
        payment.setAccountId(accountId);
        payment.setTitle("Pagamento fatura " + card.getName() + " " + month);
        payment.setValue(request.value());
        payment.setIncome(false);
        payment.setCategory(null);
        payment.setCreditCard(card);
        payment.setEntryType(TransactionEntryType.INVOICE_PAYMENT);
        payment.setInvoiceDueDate(CreditCardInvoiceCalculator.toDate(invoice.dueDate()));
        payment.setExecutionDate(CreditCardInvoiceCalculator.toDate(request.date()));
        payment.setStatus(TransactionStatus.OK);
        transactions.save(payment);

        return invoiceResponse(card, owner, month);
    }

    @Override
    public CreditCardInvoiceResponse deletePayment(UUID cardId, String invoiceMonth, UUID paymentId) {
        CreditCard card = requireCard(cardId);
        YearMonth month = parseMonth(invoiceMonth);
        UUID owner = accounts.ownerOf(card.getAccountId());
        Transaction payment = transactions.findById(paymentId)
                .orElseThrow(() -> new CustomExceptions.EntityNotFoundException("Transaction", paymentId));
        if (payment.getEntryType() != TransactionEntryType.INVOICE_PAYMENT
                || payment.getCreditCard() == null || !cardId.equals(payment.getCreditCard().getId()))
            throw new IllegalArgumentException("Transaction is not a payment of this card");
        LocalDate due = payment.getInvoiceDueDate() == null ? null
                : CreditCardInvoiceCalculator.toLocalDate(payment.getInvoiceDueDate());
        if (due == null || !YearMonth.from(due).equals(month))
            throw new IllegalArgumentException("Payment does not belong to the informed invoice");
        transactions.delete(payment);
        return invoiceResponse(card, owner, month);
    }

    @Override
    @Transactional(readOnly = true)
    public CreditCard requireCard(UUID cardId) {
        CreditCard card = cards.findById(cardId)
                .orElseThrow(() -> new CustomExceptions.EntityNotFoundException(CreditCard.class.getSimpleName(), cardId));
        accounts.requireAccess(card.getAccountId());
        return card;
    }

    private CreditCardInvoiceResponse invoiceResponse(CreditCard card, UUID owner, YearMonth month) {
        CreditCardInvoiceService.Ledger ledger = invoices.ledger(card, owner, LocalDate.now(ZoneOffset.UTC));
        return toResponse(invoices.invoice(card, ledger, month), invoices.payments(card, owner, month));
    }

    private boolean hasPendingPurchase(CreditCard card, YearMonth invoiceMonth) {
        CreditCardInvoiceCalculator.Cycle cycle = CreditCardInvoiceCalculator.cycleOfInvoiceMonth(
                invoiceMonth, card.getClosingDay(), card.getDueDay());
        // Compras do ciclo: fechamento anterior (exclusivo) até o fechamento atual (inclusivo).
        return !transactions.findPendingCardPurchasesBetween(card.getId(),
                CreditCardInvoiceCalculator.toDate(cycle.previousClosingDate().plusDays(1)),
                CreditCardInvoiceCalculator.toDate(cycle.closingDate().plusDays(1))).isEmpty();
    }

    private CreditCardResponse response(CreditCard card, UUID owner, LocalDate today) {
        BigDecimal open = invoices.openTotal(card, owner, today);
        return new CreditCardResponse(card.getId(), card.getAccountId(), card.getName(), card.getCardLimit(),
                card.getClosingDay(), card.getDueDay(),
                card.getStatus() == null ? CardStatus.ACTIVE.name() : card.getStatus().name(), open);
    }

    private static CreditCardInvoiceResponse toResponse(CreditCardInvoiceService.Invoice invoice,
                                                        List<Transaction> payments) {
        List<CreditCardInvoicePaymentResponse> paymentResponses = new ArrayList<>();
        for (Transaction payment : payments) {
            paymentResponses.add(new CreditCardInvoicePaymentResponse(payment.getId(),
                    invoice.invoiceMonth().toString(), payment.getValue(),
                    CreditCardInvoiceCalculator.toLocalDate(payment.getExecutionDate()), payment.getId()));
        }
        return new CreditCardInvoiceResponse(invoice.card().getId(), invoice.invoiceMonth().toString(),
                invoice.closingDate(), invoice.dueDate(), invoice.total(), invoice.paid(), invoice.remaining(),
                invoice.status().name(), paymentResponses);
    }

    private static YearMonth parseMonth(String invoiceMonth) {
        try {
            return YearMonth.parse(invoiceMonth);
        } catch (DateTimeParseException | NullPointerException ex) {
            throw new IllegalArgumentException("invoiceMonth must be formatted as yyyy-MM");
        }
    }

    private static void validate(String name, BigDecimal cardLimit, int closingDay, int dueDay) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Card name is required");
        if (closingDay < 1 || closingDay > 31) throw new IllegalArgumentException("closingDay must be between 1 and 31");
        if (dueDay < 1 || dueDay > 31) throw new IllegalArgumentException("dueDay must be between 1 and 31");
        if (cardLimit != null && cardLimit.signum() < 0) throw new IllegalArgumentException("cardLimit cannot be negative");
    }

    private static LocalDate today() {
        return LocalDate.now(ZoneOffset.UTC);
    }
}
