package com.silva.edu.finances.services.impl;

import com.silva.edu.finances.domain.dtos.request.CreateFinanceAccountRequest;
import com.silva.edu.finances.domain.dtos.response.FinanceAccountResponse;
import com.silva.edu.finances.domain.entities.FinanceAccount;
import com.silva.edu.finances.domain.entities.FinanceAccountMember;
import com.silva.edu.finances.domain.entities.Transaction;
import com.silva.edu.finances.domain.enums.AccountMemberStatus;
import com.silva.edu.finances.domain.enums.FinancialAccountType;
import com.silva.edu.finances.infra.config.UserContext;
import com.silva.edu.finances.infra.exceptions.CustomExceptions;
import com.silva.edu.finances.repositories.FinanceAccountMemberRepository;
import com.silva.edu.finances.repositories.FinanceAccountRepository;
import com.silva.edu.finances.repositories.TransactionRepository;
import com.silva.edu.finances.repositories.RecurrenceRepository;
import com.silva.edu.finances.services.FinanceAccountService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Date;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Service @Transactional
public class FinanceAccountServiceImpl implements FinanceAccountService {
    private final FinanceAccountRepository accounts;
    private final FinanceAccountMemberRepository members;
    private final TransactionRepository transactions;
    private final RecurrenceRepository recurrences;

    public FinanceAccountServiceImpl(FinanceAccountRepository accounts, FinanceAccountMemberRepository members,
                                     TransactionRepository transactions, RecurrenceRepository recurrences) {
        this.accounts = accounts;
        this.members = members;
        this.transactions = transactions;
        this.recurrences = recurrences;
    }
    @Override
    public List<FinanceAccountResponse> listMine() {
        UUID user = UserContext.getUser();
        getOrCreatePersonalDefault();
        List<FinanceAccountResponse> result = new ArrayList<>();
        for (FinanceAccount account : accounts.findAllByOwnerId(user)) {
            result.add(response(account, account.getOwnerId(), "OWNER"));
        }
        for (FinanceAccountMember member : members.findAllByUserIdAndStatus(user, AccountMemberStatus.ACTIVE)) {
            accounts.findById(member.getAccountId())
                    .filter(account -> !account.getOwnerId().equals(user))
                    .ifPresent(account -> result.add(response(account, account.getOwnerId(), "MEMBER")));
        }
        return result;
    }
    @Override public FinanceAccountResponse create(CreateFinanceAccountRequest request) {
        UUID user = UserContext.getUser();
        FinanceAccount account = new FinanceAccount();
        account.setId(UUID.randomUUID().toString()); account.setOwnerId(user);
        account.setName(request.name().trim()); account.setType(request.type());
        account.setOpeningBalance(request.openingBalance()); account.setCurrency("BRL");
        accounts.save(account);
        return response(account, user, "OWNER");
    }
    @Override @Transactional(readOnly = true)
    public FinanceAccountResponse get(String accountId) {
        FinanceAccount account = requireAccount(accountId);
        String role = account.getOwnerId().equals(UserContext.getUser()) ? "OWNER" : "MEMBER";
        return response(account, account.getOwnerId(), role);
    }
    @Override @Transactional(readOnly = true)
    public void requireAccess(String accountId) {
        requireAccount(accountId);
    }
    @Override @Transactional(readOnly = true)
    public FinanceAccount requireAccount(String accountId) {
        if (accountId == null || accountId.isBlank())
            throw new CustomExceptions.EntityNotFoundException("FinanceAccount", accountId);
        UUID user = UserContext.getUser();
        FinanceAccount account = accounts.findById(accountId)
                .orElseThrow(() -> new CustomExceptions.EntityNotFoundException("FinanceAccount", accountId));
        if (account.getOwnerId().equals(user)) return account;
        if (members.existsByAccountIdAndUserIdAndStatus(accountId, user, AccountMemberStatus.ACTIVE)) return account;
        throw new CustomExceptions.EntityNotFoundException("FinanceAccount", accountId);
    }
    @Override @Transactional(readOnly = true)
    public UUID ownerOf(String accountId) {
        return requireAccount(accountId).getOwnerId();
    }
    @Override @Transactional(readOnly = true)
    public List<String> accessibleAccountIds() {
        UUID user = UserContext.getUser();
        Set<String> ids = new LinkedHashSet<>();
        for (FinanceAccount account : accounts.findAllByOwnerId(user)) ids.add(account.getId());
        for (FinanceAccountMember member : members.findAllByUserIdAndStatus(user, AccountMemberStatus.ACTIVE))
            ids.add(member.getAccountId());
        return new ArrayList<>(ids);
    }
    @Override @Transactional(readOnly = true)
    public Set<UUID> visibleUserIds() {
        UUID user = UserContext.getUser();
        Set<UUID> ids = new HashSet<>();
        ids.add(user);
        for (FinanceAccountMember member : members.findAllByUserIdAndStatus(user, AccountMemberStatus.ACTIVE)) {
            accounts.findById(member.getAccountId()).ifPresent(account -> ids.add(account.getOwnerId()));
        }
        List<FinanceAccount> owned = accounts.findAllByOwnerId(user);
        List<String> ownedIds = owned.stream().map(FinanceAccount::getId).toList();
        if (!ownedIds.isEmpty()) {
            for (FinanceAccountMember member : members.findAllByAccountIdInAndStatus(ownedIds, AccountMemberStatus.ACTIVE)) {
                if (member.getUserId() != null) ids.add(member.getUserId());
            }
        }
        return ids;
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
    private FinanceAccountResponse response(FinanceAccount account, UUID dataOwner, String role) {
        BigDecimal net = transactions.sumNetByAccountAndUser(account.getId(), dataOwner, endOfToday());
        if (net == null) net = BigDecimal.ZERO;
        return new FinanceAccountResponse(account.getId(), account.getName(), account.getType(),
                account.getOpeningBalance(), account.getOpeningBalance().add(net), account.getCurrency(), role);
    }

    /**
     * "Até hoje" inclui o dia inteiro: o corte é o início de amanhã, não o instante atual.
     * Os lançamentos da web guardam a data às 12:00, então comparar com {@code new Date()}
     * escondia do caixa tudo o que fosse lançado para o dia de hoje até o meio-dia.
     */
    private static Date endOfToday() {
        return Date.from(LocalDate.now(ZoneOffset.UTC).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant());
    }
}
