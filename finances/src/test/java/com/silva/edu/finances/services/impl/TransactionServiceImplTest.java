package com.silva.edu.finances.services.impl;

import com.silva.edu.finances.domain.dtos.request.UpdateTransactionRequestDTO;
import com.silva.edu.finances.domain.entities.Category;
import com.silva.edu.finances.domain.entities.Recurrence;
import com.silva.edu.finances.domain.entities.Transaction;
import com.silva.edu.finances.domain.enums.TransactionStatus;
import com.silva.edu.finances.repositories.RecurrenceRepository;
import com.silva.edu.finances.repositories.TransactionRepository;
import com.silva.edu.finances.services.CategoryService;
import com.silva.edu.finances.services.FinanceAccountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {
    @Mock private TransactionRepository repository;
    @Mock private CategoryService categoryService;
    @Mock private RecurrenceRepository recurrenceRepository;
    @Mock private FinanceAccountService accountService;
    @InjectMocks private TransactionServiceImpl service;

    private final UUID transactionId = UUID.randomUUID();

    @Test
    void updateChangesCategoryOfAlreadyCategorizedTransaction() {
        Transaction transaction = transaction();
        transaction.setCategory(category(false, "Antiga"));
        Category newCategory = category(true, "Salário");
        when(repository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(categoryService.requireAccessible(newCategory.getId())).thenReturn(newCategory);
        when(repository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        service.update(transactionId, request(newCategory.getId(), true));

        assertThat(transaction.getCategory()).isEqualTo(newCategory);
        assertThat(transaction.isIncome()).isTrue();
        verify(repository).save(transaction);
    }

    @Test
    void updateSyncsRecurrenceAndPendingOccurrences() {
        Transaction transaction = transaction();
        transaction.setCategory(category(false, "Antiga"));
        Recurrence recurrence = new Recurrence();
        recurrence.setId(UUID.randomUUID());
        recurrence.setUserId(UUID.randomUUID());
        transaction.setRecurrence(recurrence);
        Category newCategory = category(false, "Nova");
        when(repository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(categoryService.requireAccessible(newCategory.getId())).thenReturn(newCategory);
        when(repository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        service.update(transactionId, request(newCategory.getId(), false));

        assertThat(recurrence.getCategory()).isEqualTo(newCategory);
        assertThat(recurrence.isIncome()).isFalse();
        verify(recurrenceRepository).save(recurrence);
        verify(repository).updatePendingCategoryByRecurrenceExcept(
                recurrence.getId(), recurrence.getUserId(), newCategory, false, transactionId);
    }

    @Test
    void updateRejectsCategoryWithWrongDirection() {
        Transaction transaction = transaction();
        Category category = category(false, "Mercado");
        when(repository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(categoryService.requireAccessible(category.getId())).thenReturn(category);

        assertThatThrownBy(() -> service.update(transactionId, request(category.getId(), true)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void findAllOrdersByExecutionDateDescending() {
        when(repository.findAllByAccountId(eq("ACC"), any())).thenReturn(org.springframework.data.domain.Page.empty());

        service.findAll(0, 20, "ACC");

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAllByAccountId(eq("ACC"), captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("executionDate").getDirection())
                .isEqualTo(org.springframework.data.domain.Sort.Direction.DESC);
    }

    private Transaction transaction() {
        Transaction transaction = new Transaction();
        transaction.setId(transactionId);
        transaction.setAccountId("ACC");
        transaction.setTitle("Aluguel");
        transaction.setValue(BigDecimal.TEN);
        transaction.setStatus(TransactionStatus.PENDING);
        return transaction;
    }

    private UpdateTransactionRequestDTO request(UUID categoryId, boolean income) {
        return new UpdateTransactionRequestDTO(BigDecimal.TEN, "Aluguel", null, categoryId, income, null);
    }

    private Category category(boolean income, String title) {
        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setIncome(income);
        category.setTitle(title);
        return category;
    }
}
