package com.silva.edu.finances.services.impl;

import com.silva.edu.finances.domain.dtos.request.UpdateRecurrenceRequestDTO;
import com.silva.edu.finances.domain.entities.Category;
import com.silva.edu.finances.domain.entities.Recurrence;
import com.silva.edu.finances.repositories.RecurrenceRepository;
import com.silva.edu.finances.repositories.TransactionRepository;
import com.silva.edu.finances.services.CategoryService;
import com.silva.edu.finances.services.FinanceAccountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecurrenceServiceImplTest {
    @Mock private RecurrenceRepository repository;
    @Mock private CategoryService categoryService;
    @Mock private TransactionRepository transactionRepository;
    @Mock private FinanceAccountService accountService;
    @Mock private RecurrenceMaterializer materializer;
    @InjectMocks private RecurrenceServiceImpl service;

    private final UUID recurrenceId = UUID.randomUUID();

    @Test
    void updateChangesCategoryAndPropagatesToPendingOccurrences() {
        Recurrence recurrence = new Recurrence();
        recurrence.setId(recurrenceId);
        recurrence.setUserId(UUID.randomUUID());
        recurrence.setAccountId("ACC");
        recurrence.setCategory(category(false, "Antiga"));
        Category newCategory = category(true, "Salário");
        when(repository.findById(recurrenceId)).thenReturn(Optional.of(recurrence));
        when(categoryService.requireAccessible(newCategory.getId())).thenReturn(newCategory);
        when(repository.save(recurrence)).thenReturn(recurrence);

        service.update(recurrenceId, new UpdateRecurrenceRequestDTO(
                5, "Salário", null, null, BigDecimal.TEN, newCategory.getId(), true, null));

        assertThat(recurrence.getCategory()).isEqualTo(newCategory);
        assertThat(recurrence.isIncome()).isTrue();
        verify(transactionRepository).updatePendingCategoryByRecurrence(
                recurrenceId, recurrence.getUserId(), newCategory, true);
    }

    private Category category(boolean income, String title) {
        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setIncome(income);
        category.setTitle(title);
        return category;
    }
}
