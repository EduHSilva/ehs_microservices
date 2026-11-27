package com.edu.silva.budgets.services;

import com.edu.silva.budgets.domain.dtos.requests.UpdateBudgetRequestDTO;
import com.edu.silva.budgets.domain.dtos.responses.BudgetResponseDTO;
import com.edu.silva.budgets.domain.entities.Budget;
import com.edu.silva.budgets.domain.entities.Client;
import com.edu.silva.budgets.domain.enums.BudgetStatus;
import com.edu.silva.budgets.infra.config.TenantFilter;
import com.edu.silva.budgets.infra.config.UserContext;
import com.edu.silva.budgets.infra.exceptions.CustomExceptions;
import com.edu.silva.budgets.repositories.BudgetRepository;
import com.edu.silva.budgets.repositories.ClientRepository;
import com.edu.silva.budgets.services.impl.BudgetServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BudgetServiceImplTest {

    @Mock
    private BudgetRepository repository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private TenantFilter tenantFilter;

    private BudgetServiceImpl service;
    private UUID userId;
    private UUID budgetId;
    private Budget budget;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new BudgetServiceImpl(repository, clientRepository, tenantFilter);

        userId = UUID.randomUUID();
        budgetId = UUID.randomUUID();
        UserContext.setUser(userId);

        Client client = new Client();
        client.setId(UUID.randomUUID());
        client.setName("Cliente");

        budget = new Budget();
        budget.setId(budgetId);
        budget.setUserId(userId);
        budget.setClient(client);
        budget.setTitle("Orçamento");
        budget.setStatus(BudgetStatus.WAITING);

        when(repository.findByIdAndUserId(budgetId, userId)).thenReturn(Optional.of(budget));
        when(repository.findByApprovalToken(any(UUID.class))).thenReturn(Optional.of(budget));
        when(repository.save(any(Budget.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    private UpdateBudgetRequestDTO request(BudgetStatus status) {
        return new UpdateBudgetRequestDTO("Orçamento", null, 0, List.of(), null, null, status, null);
    }

    @Test
    void shouldApplyAllowedTransition() {
        BudgetResponseDTO result = service.update(budgetId, request(BudgetStatus.APPROVED));

        assertEquals(BudgetStatus.APPROVED, result.getStatus());
        verify(repository).save(budget);
    }

    @Test
    void shouldRejectTransitionOutsideTheMatrix() {
        assertThrows(CustomExceptions.InvalidStatusException.class,
                () -> service.update(budgetId, request(BudgetStatus.DRAFT)));

        assertEquals(BudgetStatus.WAITING, budget.getStatus());
        verify(repository, never()).save(any());
    }

    @Test
    void shouldAllowKeepingTheCurrentStatus() {
        BudgetResponseDTO result = service.update(budgetId, request(BudgetStatus.WAITING));

        assertEquals(BudgetStatus.WAITING, result.getStatus());
        verify(repository).save(budget);
    }

    @Test
    void shouldAllowCancelFromAnyStatus() {
        budget.setStatus(BudgetStatus.DONE);

        BudgetResponseDTO result = service.update(budgetId, request(BudgetStatus.CANCEL));

        assertEquals(BudgetStatus.CANCEL, result.getStatus());
    }

    @Test
    void shouldRejectDeletedThroughPut() {
        assertThrows(CustomExceptions.InvalidStatusException.class,
                () -> service.update(budgetId, request(BudgetStatus.DELETED)));

        verify(repository, never()).save(any());
    }

    @Test
    void shouldApproveThroughPublicToken() {
        UUID token = UUID.randomUUID();

        BudgetResponseDTO result = service.decideByApprovalToken(token, BudgetStatus.APPROVED);

        assertEquals(BudgetStatus.APPROVED, result.getStatus());
        verify(repository).findByApprovalToken(token);
        verify(repository).save(budget);
    }

    @Test
    void shouldRejectNonDecisionThroughPublicToken() {
        assertThrows(CustomExceptions.InvalidStatusException.class,
                () -> service.decideByApprovalToken(UUID.randomUUID(), BudgetStatus.WORKING));

        verify(repository, never()).save(any());
    }

    @Test
    void shouldRejectDecisionWhenAlreadyFinished() {
        budget.setStatus(BudgetStatus.DONE);

        assertThrows(CustomExceptions.InvalidStatusException.class,
                () -> service.decideByApprovalToken(UUID.randomUUID(), BudgetStatus.DENY));

        verify(repository, never()).save(any());
    }
}
