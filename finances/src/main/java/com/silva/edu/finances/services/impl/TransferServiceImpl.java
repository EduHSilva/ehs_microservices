package com.silva.edu.finances.services.impl;

import com.silva.edu.finances.domain.dtos.request.TransferRequest;
import com.silva.edu.finances.domain.dtos.request.UpdateTransferRequest;
import com.silva.edu.finances.domain.dtos.response.TransferResponse;
import com.silva.edu.finances.domain.entities.FinanceAccount;
import com.silva.edu.finances.domain.entities.Transaction;
import com.silva.edu.finances.domain.enums.TransactionEntryType;
import com.silva.edu.finances.domain.enums.TransactionStatus;
import com.silva.edu.finances.infra.exceptions.CustomExceptions;
import com.silva.edu.finances.repositories.TransactionRepository;
import com.silva.edu.finances.services.FinanceAccountService;
import com.silva.edu.finances.services.TransferService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Transferência = par de lançamentos TRANSFER com o mesmo transferId (saída na origem, entrada no
 * destino). Não é receita nem despesa: só move o saldo das duas contas.
 */
@Service
@Transactional
public class TransferServiceImpl implements TransferService {
    private static final String CURRENCY = "BRL";

    private final TransactionRepository transactions;
    private final FinanceAccountService accounts;

    public TransferServiceImpl(TransactionRepository transactions, FinanceAccountService accounts) {
        this.transactions = transactions;
        this.accounts = accounts;
    }

    @Override
    public TransferResponse create(TransferRequest request) {
        if (request.value() == null || request.value().signum() <= 0)
            throw new IllegalArgumentException("Transfer value must be greater than zero");
        if (request.date() == null) throw new IllegalArgumentException("Transfer date is required");
        String fromId = request.fromAccountId() == null ? null : request.fromAccountId().trim();
        String toId = request.toAccountId() == null ? null : request.toAccountId().trim();
        FinanceAccount from = accounts.requireAccount(fromId);
        FinanceAccount to = accounts.requireAccount(toId);
        if (from.getId().equals(to.getId()))
            throw new IllegalArgumentException("Transfer accounts must be different");
        if (!CURRENCY.equals(from.getCurrency()) || !CURRENCY.equals(to.getCurrency()))
            throw new IllegalArgumentException("Transfers are only supported between BRL accounts");

        Date date = CreditCardInvoiceCalculator.toDate(request.date());
        String customTitle = request.title() == null || request.title().isBlank() ? null : request.title().trim();
        Transaction out = leg(from, UUID.randomUUID(), request.value(), date,
                customTitle == null ? "Transferência para " + to.getName() : customTitle, false);
        transactions.save(out);
        Transaction in = leg(to, out.getTransferId(), request.value(), date,
                customTitle == null ? "Transferência de " + from.getName() : customTitle, true);
        transactions.save(in);
        return response(from.getId(), to.getId(), out, in);
    }

    @Override
    public TransferResponse update(UUID transferId, UpdateTransferRequest request) {
        if (request.value() == null || request.value().signum() <= 0)
            throw new IllegalArgumentException("Transfer value must be greater than zero");
        if (request.date() == null) throw new IllegalArgumentException("Transfer date is required");
        List<Transaction> legs = transferLegs(transferId);
        Transaction out = outgoing(legs);
        Transaction in = incoming(legs);
        Date date = CreditCardInvoiceCalculator.toDate(request.date());
        for (Transaction leg : List.of(out, in)) {
            leg.setValue(request.value());
            leg.setExecutionDate(date);
            if (request.title() != null && !request.title().isBlank()) leg.setTitle(request.title().trim());
            transactions.save(leg);
        }
        return response(out.getAccountId(), in.getAccountId(), out, in);
    }

    @Override
    public void delete(UUID transferId) {
        for (Transaction leg : transferLegs(transferId)) transactions.delete(leg);
    }

    @Override
    @Transactional(readOnly = true)
    public TransferResponse find(UUID transferId) {
        List<Transaction> legs = transferLegs(transferId);
        Transaction out = outgoing(legs);
        return response(out.getAccountId(), incoming(legs).getAccountId(), out, incoming(legs));
    }

    private Transaction leg(FinanceAccount account, UUID transferId, java.math.BigDecimal value, Date date,
                            String title, boolean income) {
        Transaction leg = new Transaction();
        leg.setUserId(account.getOwnerId());
        leg.setAccountId(account.getId());
        leg.setTitle(title);
        leg.setValue(value);
        leg.setIncome(income);
        leg.setCategory(null);
        leg.setEntryType(TransactionEntryType.TRANSFER);
        leg.setTransferId(transferId);
        leg.setExecutionDate(date);
        leg.setStatus(TransactionStatus.OK);
        return leg;
    }

    private List<Transaction> transferLegs(UUID transferId) {
        List<Transaction> legs = transactions.findByTransferId(transferId).stream()
                .filter(leg -> leg.getEntryType() == TransactionEntryType.TRANSFER)
                .toList();
        if (legs.size() != 2 || legs.stream().anyMatch(leg -> leg.getStatus() != TransactionStatus.OK))
            throw new CustomExceptions.EntityNotFoundException("Transfer", transferId);
        for (Transaction leg : legs) accounts.requireAccess(leg.getAccountId());
        return legs;
    }

    private static Transaction outgoing(List<Transaction> legs) {
        return legs.stream().filter(leg -> !leg.isIncome()).findFirst()
                .orElseThrow(() -> new IllegalStateException("Transfer without outgoing leg"));
    }

    private static Transaction incoming(List<Transaction> legs) {
        return legs.stream().filter(Transaction::isIncome).findFirst()
                .orElseThrow(() -> new IllegalStateException("Transfer without incoming leg"));
    }

    private static TransferResponse response(String fromAccountId, String toAccountId, Transaction out, Transaction in) {
        LocalDate date = out.getExecutionDate().toInstant().atZone(ZoneOffset.UTC).toLocalDate();
        return new TransferResponse(out.getTransferId(), fromAccountId, toAccountId, out.getValue(), date,
                out.getTitle(), out.getStatus().name(), out.getId(), in.getId());
    }
}
