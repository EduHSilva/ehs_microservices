package com.silva.edu.finances.services.impl;

import com.silva.edu.finances.domain.dtos.request.CreateFinanceAccountRequest;
import com.silva.edu.finances.domain.dtos.response.FinanceAccountResponse;
import com.silva.edu.finances.domain.entities.FinanceAccount;
import com.silva.edu.finances.domain.entities.Transaction;
import com.silva.edu.finances.domain.enums.FinancialAccountType;
import com.silva.edu.finances.infra.config.UserContext;
import com.silva.edu.finances.infra.exceptions.CustomExceptions;
import com.silva.edu.finances.repositories.FinanceAccountRepository;
import com.silva.edu.finances.repositories.TransactionRepository;
import com.silva.edu.finances.repositories.RecurrenceRepository;
import com.silva.edu.finances.services.FinanceAccountService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.Date;

@Service @Transactional
public class FinanceAccountServiceImpl implements FinanceAccountService {
    private final FinanceAccountRepository accounts;
    private final TransactionRepository transactions;
    private final RecurrenceRepository recurrences;

    public FinanceAccountServiceImpl(FinanceAccountRepository accounts, TransactionRepository transactions, RecurrenceRepository recurrences) {
        this.accounts = accounts;
        this.transactions = transactions;
        this.recurrences = recurrences;
    }
    @Override
    public List<FinanceAccountResponse> listMine() {
        UUID user = UserContext.getUser();
        getOrCreatePersonalDefault();
        return accounts.findAllByOwnerId(user).stream().map(account -> response(account, user)).toList();
    }
    @Override public FinanceAccountResponse create(CreateFinanceAccountRequest request) {
        UUID user = UserContext.getUser();
        FinanceAccount account = new FinanceAccount();
        account.setId(UUID.randomUUID().toString()); account.setOwnerId(user);
        account.setName(request.name().trim()); account.setType(request.type());
        account.setOpeningBalance(request.openingBalance()); account.setCurrency("BRL");
        accounts.save(account);
        return response(account, user);
    }
    @Override @Transactional(readOnly = true)
    public FinanceAccountResponse get(String accountId) {
        UUID user = UserContext.getUser();
        FinanceAccount account = accounts.findByIdAndOwnerId(accountId, user)
                .orElseThrow(() -> new CustomExceptions.EntityNotFoundException("FinanceAccount", accountId));
        return response(account, user);
    }
    @Override @Transactional(readOnly = true)
    public void requireAccess(String accountId) {
        if (accountId == null || accountId.isBlank() || accounts.findByIdAndOwnerId(accountId, UserContext.getUser()).isEmpty())
            throw new CustomExceptions.EntityNotFoundException("FinanceAccount", accountId);
    }
    @Override public String getOrCreatePersonalDefault() {
        UUID user = UserContext.getUser(); String id = "PF-" + user;
        FinanceAccount account = accounts.findByIdAndOwnerId(id, user).orElse(null);
        if (account == null) {
            account = accounts.findById(id).orElseGet(() -> {
                FinanceAccount created = new FinanceAccount(); created.setId(id); created.setOwnerId(user);
                created.setName("Conta pessoal"); created.setType(FinancialAccountType.PF);
                created.setOpeningBalance(BigDecimal.ZERO); created.setCurrency("BRL"); return accounts.save(created);
            });
            if (!account.getOwnerId().equals(user)) throw new CustomExceptions.EntityNotFoundException("FinanceAccount", id);
        }
        if (!account.isLegacyDataMigrated()) {
            // Records created before accountId/direction existed are assigned to the user's PF account.
            transactions.assignLegacyTransactions(user, id);
            recurrences.assignLegacyRecurrences(user, id);
            int page = 0;
            Page<Transaction> batch;
            do {
                batch = transactions.findAllByUserIdAndRecurrenceIsNotNull(user, PageRequest.of(page++, 500));
                for (Transaction transaction : batch.getContent()) transaction.setIncome(transaction.getRecurrence().isIncome());
                transactions.saveAll(batch.getContent());
            } while (batch.hasNext());
            account.setLegacyDataMigrated(true);
            accounts.save(account);
        }
        return id;
    }
    private FinanceAccountResponse response(FinanceAccount account, UUID user) {
        BigDecimal net = transactions.sumNetByAccountAndUser(account.getId(), user, new Date());
        if (net == null) net = BigDecimal.ZERO;
        return new FinanceAccountResponse(account.getId(), account.getName(), account.getType(),
                account.getOpeningBalance(), account.getOpeningBalance().add(net), account.getCurrency());
    }
}
