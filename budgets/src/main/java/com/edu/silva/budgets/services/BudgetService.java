package com.edu.silva.budgets.services;

import com.edu.silva.budgets.domain.dtos.requests.NewBudgetRequestDTO;
import com.edu.silva.budgets.domain.dtos.requests.UpdateBudgetRequestDTO;
import com.edu.silva.budgets.domain.dtos.responses.BudgetResponseDTO;
import com.edu.silva.budgets.domain.enums.BudgetStatus;
import lombok.NonNull;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface BudgetService {
    BudgetResponseDTO save(NewBudgetRequestDTO request);
    void delete(UUID id);
    Page<@NonNull BudgetResponseDTO> findAll(int page, int size, String title, String status);
    BudgetResponseDTO findById(UUID id);
    BudgetResponseDTO update(UUID id, UpdateBudgetRequestDTO request);

    /** Busca pública pelo token de aprovação, sem contexto de usuário. */
    BudgetResponseDTO findByApprovalToken(UUID token);

    /** Aprova ou recusa o orçamento pelo token público. */
    BudgetResponseDTO decideByApprovalToken(UUID token, BudgetStatus decision);
}
