package com.silva.edu.finances.services.impl;

import com.silva.edu.finances.domain.dtos.request.AddRecurrenceRequestDTO;
import com.silva.edu.finances.domain.dtos.request.UpdateRecurrenceRequestDTO;
import com.silva.edu.finances.domain.dtos.response.RecurrenceResponseDTO;
import com.silva.edu.finances.domain.entities.Category;
import com.silva.edu.finances.domain.entities.Recurrence;
import com.silva.edu.finances.domain.enums.RecurrenceStatus;
import com.silva.edu.finances.domain.enums.RecurrenceType;
import com.silva.edu.finances.infra.exceptions.CustomExceptions;
import com.silva.edu.finances.repositories.CategoryRepository;
import com.silva.edu.finances.repositories.RecurrenceRepository;
import com.silva.edu.finances.services.RecurrenceService;
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
public class RecurrenceServiceImpl implements RecurrenceService {

    private final RecurrenceRepository repository;
    private final CategoryRepository categoryRepository;
    private final FinanceAccountService accountService;

    public RecurrenceServiceImpl(RecurrenceRepository repository, CategoryRepository categoryRepository, FinanceAccountService accountService) {
        this.repository = repository;
        this.categoryRepository = categoryRepository;
        this.accountService = accountService;
    }

    @Override
    public RecurrenceResponseDTO save(AddRecurrenceRequestDTO request) {
        Recurrence r = new Recurrence();
        BeanUtils.copyProperties(request, r);
        String accountId = request.accountId() == null || request.accountId().isBlank()
                ? accountService.getOrCreatePersonalDefault() : request.accountId().trim();
        accountService.requireAccess(accountId);
        r.setAccountId(accountId);
        r.setStatus(request.status() == null ? RecurrenceStatus.ACTIVE : request.status());
        if (r.getStatus() == RecurrenceStatus.DELETED)
            throw new IllegalArgumentException("Recurrences cannot be created as DELETED");
        validateInstallments(request.type(), request.installments(), request.payed());

        Category category = findCategory(request.categoryID());
        requireCategoryDirection(category, request.income());
        r.setCategory(category);
        r.setUserId(UserContext.getUser());
        repository.save(r);
        return new RecurrenceResponseDTO(r);
    }

    @Override
    public void delete(UUID id) {
        repository.delete(findEntity(id));
    }

    @Override
    public Page<@NonNull RecurrenceResponseDTO> findAll(int page, int size, String accountId) {
        if (accountId != null && !accountId.isBlank()) accountService.requireAccess(accountId);
        Pageable pageable = PageRequest.of(page, size);
        var result = accountId == null || accountId.isBlank()
                ? repository.findAll(pageable)
                : repository.findAllByAccountId(accountId, pageable);
        return result.map(RecurrenceResponseDTO::new);
    }

    @Override
    public RecurrenceResponseDTO findById(UUID id) {
        return new RecurrenceResponseDTO(findEntity(id));
    }

    @Override
    public RecurrenceResponseDTO update(UUID id, UpdateRecurrenceRequestDTO request) {
        Recurrence recurrence = findEntity(id);
        validateInstallments(recurrence.getType(), request.installments(), request.payed());
        recurrence.setDay(request.day());
        recurrence.setInstallments(request.installments());
        recurrence.setPayed(request.payed());
        recurrence.setTitle(request.title());
        recurrence.setValue(request.value());
        boolean hasCategory = recurrence.getCategory() != null;
        if (hasCategory && (request.categoryID() != null || request.income() != null))
            throw new IllegalArgumentException("Category and type can only be changed when the recurrence has no category");
        if (!hasCategory) {
            if (request.categoryID() != null) {
                Category category = findCategory(request.categoryID());
                requireCategoryDirection(category, request.income() == null ? category.isIncome() : request.income());
                recurrence.setCategory(category);
                recurrence.setIncome(category.isIncome());
            } else if (request.income() != null) {
                recurrence.setIncome(request.income());
            }
        }
        if (request.status() != null) {
            if (request.status() == RecurrenceStatus.DELETED)
                throw new IllegalArgumentException("Use the delete endpoint to remove a recurrence");
            recurrence.setStatus(request.status());
        }
        return new RecurrenceResponseDTO(repository.save(recurrence));
    }

    @Override
    public RecurrenceResponseDTO confirm(UUID id) {
        Recurrence recurrence = findEntity(id);
        if (recurrence.getStatus() != RecurrenceStatus.PENDING)
            throw new IllegalArgumentException("Only pending recurrences can be confirmed");
        recurrence.setStatus(RecurrenceStatus.ACTIVE);
        return new RecurrenceResponseDTO(repository.save(recurrence));
    }

    private Recurrence findEntity(UUID id) {
        return repository.findByIdAndUserId(id, UserContext.getUser())
                .orElseThrow(() -> new CustomExceptions.EntityNotFoundException(Recurrence.class.getSimpleName(), id));
    }

    private Category findCategory(UUID categoryId) {
        return categoryRepository.findByIdAndUserId(categoryId, UserContext.getUser())
                .orElseThrow(() -> new CustomExceptions.EntityNotFoundException(Category.class.getSimpleName(), categoryId));
    }

    private void requireCategoryDirection(Category category, boolean income) {
        if (category.isIncome() != income)
            throw new IllegalArgumentException(income
                    ? "Income recurrences require an income category"
                    : "Expense recurrences require an expense category");
    }

    private void validateInstallments(RecurrenceType type, Integer installments, Integer payed) {
        if (type == RecurrenceType.INSTALLMENT
                && (installments == null || installments <= 0 || (payed != null && payed > installments))) {
            throw new CustomExceptions.InvalidInstallmentsException();
        }
    }
}
