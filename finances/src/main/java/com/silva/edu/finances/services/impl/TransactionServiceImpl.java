package com.silva.edu.finances.services.impl;

import com.silva.edu.finances.domain.dtos.request.AddTransactionRequestDTO;
import com.silva.edu.finances.domain.dtos.request.UpdateTransactionRequestDTO;
import com.silva.edu.finances.domain.dtos.response.TransactionResponseDTO;
import com.silva.edu.finances.domain.entities.Category;
import com.silva.edu.finances.domain.entities.Recurrence;
import com.silva.edu.finances.domain.entities.Transaction;
import com.silva.edu.finances.domain.enums.RecurrenceStatus;
import com.silva.edu.finances.domain.enums.RecurrenceType;
import com.silva.edu.finances.domain.enums.TransactionStatus;
import com.silva.edu.finances.infra.exceptions.CustomExceptions;
import com.silva.edu.finances.repositories.CategoryRepository;
import com.silva.edu.finances.repositories.RecurrenceRepository;
import com.silva.edu.finances.repositories.TransactionRepository;
import com.silva.edu.finances.services.TransactionService;
import com.silva.edu.finances.services.FinanceAccountService;
import com.silva.edu.finances.infra.config.UserContext;
import lombok.NonNull;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository repository;
    private final CategoryRepository categoryRepository;
    private final RecurrenceRepository recurrenceRepository;
    private final FinanceAccountService accountService;

    public TransactionServiceImpl(TransactionRepository repository, CategoryRepository categoryRepository, RecurrenceRepository recurrenceRepository, FinanceAccountService accountService) {
        this.repository = repository;
        this.categoryRepository = categoryRepository;
        this.recurrenceRepository = recurrenceRepository;
        this.accountService = accountService;
    }

    @Override
    public TransactionResponseDTO save(AddTransactionRequestDTO request) {
        Transaction transaction = new Transaction();
        BeanUtils.copyProperties(request, transaction);
        transaction.setStatus(request.status() == null ? TransactionStatus.OK : request.status());
        if (transaction.getStatus() == TransactionStatus.DELETED)
            throw new IllegalArgumentException("Transactions cannot be created as DELETED");
        String accountId = request.accountId() == null || request.accountId().isBlank()
                ? accountService.getOrCreatePersonalDefault() : request.accountId().trim();
        accountService.requireAccess(accountId);
        transaction.setAccountId(accountId);

        if (request.categoryID() != null) {
            Category category = findCategory(request.categoryID());
            requireCategoryDirection(category, request.income());
            transaction.setCategory(category);
        }

        if (request.recurrencyID() != null) {
            Recurrence rec = recurrenceRepository.findByIdAndUserId(request.recurrencyID(), UserContext.getUser())
                    .orElseThrow(() -> new CustomExceptions.EntityNotFoundException(Recurrence.class.getSimpleName(), request.recurrencyID()));
            transaction.setRecurrence(rec);

        }
        transaction.setUserId(UserContext.getUser());

        repository.save(transaction);
        return new TransactionResponseDTO(transaction);
    }

    @Override
    public void delete(UUID id) {
        repository.delete(findEntity(id));
    }

    @Override
    public Page<@NonNull TransactionResponseDTO> findAll(int page, int size, String accountId) {
        if (accountId != null && !accountId.isBlank()) accountService.requireAccess(accountId);
        Pageable pageable = PageRequest.of(page, size);
        var result = accountId == null || accountId.isBlank()
                ? repository.findAll(pageable)
                : repository.findAllByAccountId(accountId, pageable);
        return result.map(TransactionResponseDTO::new);
    }

    @Override
    public TransactionResponseDTO findById(UUID id) {
        return new TransactionResponseDTO(findEntity(id));
    }

    @Override
    public TransactionResponseDTO update(UUID id, UpdateTransactionRequestDTO request) {
        Transaction transaction = findEntity(id);
        transaction.setValue(request.value());
        transaction.setTitle(request.title());
        if (request.executionDate() != null) {
            transaction.setExecutionDate(request.executionDate());
        }
        boolean hasCategory = transaction.getCategory() != null;
        if (hasCategory && (request.categoryID() != null || request.income() != null))
            throw new IllegalArgumentException("Category and type can only be changed when the transaction has no category");
        if (!hasCategory) {
            if (request.categoryID() != null) {
                Category category = findCategory(request.categoryID());
                requireCategoryDirection(category, request.income() == null ? category.isIncome() : request.income());
                transaction.setCategory(category);
                transaction.setIncome(category.isIncome());
            } else if (request.income() != null) {
                transaction.setIncome(request.income());
            }
        }
        if (request.status() != null) {
            if (request.status() == TransactionStatus.DELETED)
                throw new IllegalArgumentException("Use the delete endpoint to remove a transaction");
            transaction.setStatus(request.status());
        }
        return new TransactionResponseDTO(repository.save(transaction));
    }

    @Override
    public TransactionResponseDTO confirm(UUID id, java.math.BigDecimal value) {
        Transaction transaction = findEntity(id);
        if (transaction.getStatus() != TransactionStatus.PENDING)
            throw new IllegalArgumentException("Only pending transactions can be confirmed");
        if (value != null) {
            if (value.signum() <= 0)
                throw new IllegalArgumentException("Transaction value must be greater than zero");
            transaction.setValue(value);
        }
        transaction.setStatus(TransactionStatus.OK);
        Recurrence recurrence = transaction.getRecurrence();
        if (recurrence != null && recurrence.getType() == RecurrenceType.INSTALLMENT) {
            int payed = (recurrence.getPayed() == null ? 0 : recurrence.getPayed()) + 1;
            recurrence.setPayed(payed);
            if (recurrence.getInstallments() != null && payed >= recurrence.getInstallments()) {
                recurrence.setStatus(RecurrenceStatus.PAYED);
            }
            recurrenceRepository.save(recurrence);
        }
        return new TransactionResponseDTO(repository.save(transaction));
    }

    private Transaction findEntity(UUID id) {
        return repository.findByIdAndUserId(id, UserContext.getUser())
                .orElseThrow(() -> new CustomExceptions.EntityNotFoundException(Transaction.class.getSimpleName(), id));
    }

    private Category findCategory(UUID categoryId) {
        return categoryRepository.findByIdAndUserId(categoryId, UserContext.getUser())
                .orElseThrow(() -> new CustomExceptions.EntityNotFoundException(Category.class.getSimpleName(), categoryId));
    }

    private void requireCategoryDirection(Category category, boolean income) {
        if (category.isIncome() != income)
            throw new IllegalArgumentException(income
                    ? "Income transactions require an income category"
                    : "Expense transactions require an expense category");
    }
}
