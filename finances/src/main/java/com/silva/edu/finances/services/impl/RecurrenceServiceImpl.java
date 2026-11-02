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

    public RecurrenceServiceImpl(RecurrenceRepository repository, CategoryRepository categoryRepository) {
        this.repository = repository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public RecurrenceResponseDTO save(AddRecurrenceRequestDTO request) {
        Recurrence r = new Recurrence();
        BeanUtils.copyProperties(request, r);
        r.setStatus(RecurrenceStatus.ACTIVE);
        validateInstallments(request.type(), request.installments(), request.payed());

        Category category = categoryRepository.findByIdAndUserId(request.categoryID(), UserContext.getUser())
                .orElseThrow(() -> new CustomExceptions.EntityNotFoundException(Category.class.getSimpleName(), request.categoryID()));

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
    public Page<@NonNull RecurrenceResponseDTO> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return repository.findAll(pageable).map(RecurrenceResponseDTO::new);
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
        return new RecurrenceResponseDTO(repository.save(recurrence));
    }

    private Recurrence findEntity(UUID id) {
        return repository.findByIdAndUserId(id, UserContext.getUser())
                .orElseThrow(() -> new CustomExceptions.EntityNotFoundException(Recurrence.class.getSimpleName(), id));
    }

    private void validateInstallments(RecurrenceType type, Integer installments, Integer payed) {
        if (type == RecurrenceType.INSTALLMENT
                && (installments == null || installments <= 0 || (payed != null && payed > installments))) {
            throw new CustomExceptions.InvalidInstallmentsException();
        }
    }
}
