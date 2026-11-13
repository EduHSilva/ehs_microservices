package com.silva.edu.finances.services.impl;

import com.silva.edu.finances.domain.dtos.request.StatementImportLineRequest;
import com.silva.edu.finances.domain.dtos.request.StatementImportRequest;
import com.silva.edu.finances.domain.dtos.response.StatementImportPreviewResponse;
import com.silva.edu.finances.domain.dtos.response.StatementImportResultResponse;
import com.silva.edu.finances.domain.dtos.request.ReviewImportedTransactionRequest;
import com.silva.edu.finances.domain.dtos.response.TransactionResponseDTO;
import com.silva.edu.finances.domain.dtos.response.TransactionImportAuditResponse;
import com.silva.edu.finances.domain.entities.Transaction;
import com.silva.edu.finances.domain.entities.TransactionImportAudit;
import com.silva.edu.finances.domain.enums.TransactionStatus;
import com.silva.edu.finances.infra.config.UserContext;
import com.silva.edu.finances.infra.exceptions.CustomExceptions;
import com.silva.edu.finances.repositories.CategoryRepository;
import com.silva.edu.finances.repositories.TransactionRepository;
import com.silva.edu.finances.repositories.TransactionImportAuditRepository;
import com.silva.edu.finances.services.StatementImportService;
import com.silva.edu.finances.services.FinanceAccountService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneOffset;
import java.util.*;

@Service
@Transactional
public class StatementImportServiceImpl implements StatementImportService {
    private final TransactionRepository transactions;
    private final CategoryRepository categories;
    private final FinanceAccountService accountService;
    private final TransactionImportAuditRepository auditRepository;

    public StatementImportServiceImpl(TransactionRepository transactions, CategoryRepository categories, FinanceAccountService accountService, TransactionImportAuditRepository auditRepository) {
        this.transactions = transactions;
        this.categories = categories;
        this.accountService = accountService;
        this.auditRepository = auditRepository;
    }

    @Override @Transactional(readOnly = true)
    public StatementImportPreviewResponse preview(StatementImportRequest request) {
        accountService.requireAccess(request.accountId());
        UUID userId = UserContext.getUser();
        Set<String> batch = new HashSet<>();
        List<StatementImportPreviewResponse.Line> lines = new ArrayList<>();
        int duplicates = 0;
        for (int i = 0; i < request.lines().size(); i++) {
            StatementImportLineRequest line = request.lines().get(i);
            String key = key(request.accountId(), line);
            boolean duplicate = !batch.add(key) || transactions.importKeyExists(userId, request.accountId(), key);
            if (duplicate) duplicates++;
            lines.add(new StatementImportPreviewResponse.Line(i + 1, key, line.description().trim(),
                    line.amount(), line.income(), line.date(), duplicate));
        }
        return new StatementImportPreviewResponse(lines, lines.size() - duplicates, duplicates);
    }

    @Override
    public StatementImportResultResponse confirm(StatementImportRequest request) {
        accountService.requireAccess(request.accountId());
        UUID userId = UserContext.getUser();
        Set<String> seen = new HashSet<>();
        int imported = 0;
        int duplicates = 0;
        for (StatementImportLineRequest line : request.lines()) {
            String key = key(request.accountId(), line);
            if (!seen.add(key)) {
                duplicates++;
                continue;
            }
            if (transactions.importKeyExists(userId, request.accountId(), key)) {
                duplicates++;
                continue;
            }
            Optional<Transaction> deleted = transactions.findDeletedByImportKey(userId, request.accountId(), key);
            if (deleted.isPresent()) {
                Transaction transaction = deleted.get();
                transaction.setImportSource(request.source());
                transaction.setTitle(line.description().trim());
                transaction.setValue(line.amount());
                transaction.setIncome(line.income());
                transaction.setExecutionDate(Date.from(line.date().atStartOfDay().toInstant(ZoneOffset.UTC)));
                transaction.setCategory(line.categoryId() == null ? null : categories
                        .findByIdAndUserId(line.categoryId(), userId)
                        .orElseThrow(() -> new CustomExceptions.EntityNotFoundException("Category", line.categoryId())));
                // Sai de DELETED e volta para a revisão, reaproveitando a chave única.
                transaction.setStatus(TransactionStatus.REVIEW);
                transactions.save(transaction);
                record(transaction, "IMPORTED", userId);
                imported++;
                continue;
            }
            Transaction transaction = new Transaction();
            transaction.setUserId(userId);
            transaction.setAccountId(request.accountId().trim());
            transaction.setImportKey(key);
            transaction.setImportSource(request.source());
            transaction.setTitle(line.description().trim());
            transaction.setValue(line.amount());
            transaction.setIncome(line.income());
            transaction.setExecutionDate(Date.from(line.date().atStartOfDay().toInstant(ZoneOffset.UTC)));
            transaction.setStatus(TransactionStatus.REVIEW);
            if (line.categoryId() != null) {
                transaction.setCategory(categories.findByIdAndUserId(line.categoryId(), userId)
                        .orElseThrow(() -> new CustomExceptions.EntityNotFoundException("Category", line.categoryId())));
            }
            transactions.save(transaction);
            record(transaction, "IMPORTED", userId);
            imported++;
        }
        return new StatementImportResultResponse(imported, duplicates);
    }

    @Override
    public TransactionResponseDTO review(UUID transactionId, ReviewImportedTransactionRequest request) {
        UUID userId = UserContext.getUser();
        Transaction transaction = transactions.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new CustomExceptions.EntityNotFoundException("Transaction", transactionId));
        if (transaction.getImportKey() == null || transaction.getStatus() != TransactionStatus.REVIEW)
            throw new IllegalArgumentException("Transaction is not awaiting import review");
        if (!request.approved()) {
            transactions.delete(transaction);
            record(transaction, "DISCARDED", userId);
            return null;
        }
        if (request.categoryId() != null) {
            transaction.setCategory(categories.findByIdAndUserId(request.categoryId(), userId)
                    .orElseThrow(() -> new CustomExceptions.EntityNotFoundException("Category", request.categoryId())));
        }
        transaction.setStatus(TransactionStatus.OK);
        record(transaction, "APPROVED", userId);
        return new TransactionResponseDTO(transactions.save(transaction));
    }

    @Override @Transactional(readOnly = true)
    public List<TransactionImportAuditResponse> audit(UUID transactionId) {
        UUID userId = UserContext.getUser();
        transactions.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new CustomExceptions.EntityNotFoundException("Transaction", transactionId));
        return auditRepository.findAllByTransactionIdAndUserIdOrderByCreatedAtAsc(transactionId, userId)
                .stream().map(a -> new TransactionImportAuditResponse(a.getId(), a.getTransactionId(), a.getEvent(), a.getCreatedAt())).toList();
    }

    private void record(Transaction transaction, String event, UUID userId) {
        TransactionImportAudit audit = new TransactionImportAudit();
        audit.setTransactionId(transaction.getId()); audit.setUserId(userId); audit.setEvent(event);
        auditRepository.save(audit);
    }

    private String key(String accountId, StatementImportLineRequest line) {
        String stableReference = line.sourceTransactionId() == null || line.sourceTransactionId().isBlank()
                ? line.date() + "|" + line.amount().stripTrailingZeros().toPlainString() + "|" + line.income()
                    + "|" + line.description().trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ")
                : "ref|" + line.sourceTransactionId().trim();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((accountId.trim() + "|" + stableReference).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
