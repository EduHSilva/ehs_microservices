package com.silva.edu.finances.services.impl;

import com.silva.edu.finances.domain.dtos.request.AddTransactionRequestDTO;
import com.silva.edu.finances.domain.dtos.request.UpdateTransactionRequestDTO;
import com.silva.edu.finances.domain.dtos.response.TransactionResponseDTO;
import com.silva.edu.finances.domain.entities.Category;
import com.silva.edu.finances.domain.entities.Recurrence;
import com.silva.edu.finances.domain.entities.Transaction;
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
        transaction.setStatus(TransactionStatus.OK);
        String accountId = request.accountId() == null || request.accountId().isBlank()
                ? accountService.getOrCreatePersonalDefault() : request.accountId().trim();
        accountService.requireAccess(accountId);
        transaction.setAccountId(accountId);

        Category category = categoryRepository.findByIdAndUserId(request.categoryID(), UserContext.getUser())
                .orElseThrow(() -> new CustomExceptions.EntityNotFoundException(Category.class.getSimpleName(), request.categoryID()));
        transaction.setCategory(category);

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
        return new TransactionResponseDTO(repository.save(transaction));
    }

    private Transaction findEntity(UUID id) {
        return repository.findByIdAndUserId(id, UserContext.getUser())
                .orElseThrow(() -> new CustomExceptions.EntityNotFoundException(Transaction.class.getSimpleName(), id));
    }
}
