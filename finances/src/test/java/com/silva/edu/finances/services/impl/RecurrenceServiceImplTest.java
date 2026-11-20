package com.silva.edu.finances.services.impl;

import com.silva.edu.finances.domain.dtos.request.UpdateRecurrenceRequestDTO;
import com.silva.edu.finances.domain.entities.Category;
import com.silva.edu.finances.domain.entities.CreditCard;
import com.silva.edu.finances.domain.entities.Recurrence;
import com.silva.edu.finances.domain.enums.TransactionEntryType;
import com.silva.edu.finances.repositories.CreditCardRepository;
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
    @Mock private CreditCardRepository creditCardRepository;
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
                5, "Salário", null, null, BigDecimal.TEN, newCategory.getId(), true, null, null, false));

        assertThat(recurrence.getCategory()).isEqualTo(newCategory);
        assertThat(recurrence.isIncome()).isTrue();
        verify(transactionRepository).updatePendingCategoryByRecurrence(
                recurrenceId, recurrence.getUserId(), newCategory, true);
    }

    @Test
    void updateWithCreditCardPropagatesCardToPendingOccurrences() {
        Recurrence recurrence = new Recurrence();
        recurrence.setId(recurrenceId);
        recurrence.setUserId(UUID.randomUUID());
        recurrence.setAccountId("ACC");
        recurrence.setCategory(category(false, "Mercado"));
        CreditCard card = new CreditCard();
        card.setId(UUID.randomUUID());
        card.setAccountId("ACC");
        card.setName("Cartão");
        when(repository.findById(recurrenceId)).thenReturn(Optional.of(recurrence));
        when(creditCardRepository.findById(card.getId())).thenReturn(Optional.of(card));
        when(repository.save(recurrence)).thenReturn(recurrence);

        service.update(recurrenceId, new UpdateRecurrenceRequestDTO(5, "Mercado", null, null, BigDecimal.TEN,
                null, false, null, card.getId(), false));

        assertThat(recurrence.getCreditCard()).isEqualTo(card);
        verify(transactionRepository).updatePendingCardByRecurrence(
                recurrenceId, recurrence.getUserId(), TransactionEntryType.CARD, card);
    }

    @Test
    void updateRejectsIncomeRecurrenceOnCard() {
        Recurrence recurrence = new Recurrence();
        recurrence.setId(recurrenceId);
        recurrence.setUserId(UUID.randomUUID());
        recurrence.setAccountId("ACC");
        Category mercado = category(false, "Mercado");
        recurrence.setCategory(mercado);
        when(repository.findById(recurrenceId)).thenReturn(Optional.of(recurrence));
        when(categoryService.requireAccessible(mercado.getId())).thenReturn(mercado);

        CreditCard card = new CreditCard();
        card.setId(UUID.randomUUID());
        card.setAccountId("ACC");

        // Mantendo a categoria de despesa e enviando income=true, a recorrência vira receita e o
        // vínculo com cartão (só despesa no v1) é rejeitado.
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.update(recurrenceId,
                        new UpdateRecurrenceRequestDTO(5, "Mercado", null, null, BigDecimal.TEN,
                                mercado.getId(), true, null, card.getId(), false)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repository, org.mockito.Mockito.never()).save(recurrence);
    }

    private Category category(boolean income, String title) {
        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setIncome(income);
        category.setTitle(title);
        return category;
    }
}
