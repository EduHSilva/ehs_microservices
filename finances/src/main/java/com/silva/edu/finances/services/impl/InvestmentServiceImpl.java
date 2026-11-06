package com.silva.edu.finances.services.impl;

import com.silva.edu.finances.domain.dtos.request.InvestmentMovementRequest;
import com.silva.edu.finances.domain.dtos.response.InvestmentMovementResponse;
import com.silva.edu.finances.domain.dtos.response.InvestmentSummaryResponse;
import com.silva.edu.finances.domain.entities.InvestmentMovement;
import com.silva.edu.finances.domain.enums.InvestmentMovementType;
import com.silva.edu.finances.infra.config.UserContext;
import com.silva.edu.finances.infra.exceptions.CustomExceptions;
import com.silva.edu.finances.repositories.InvestmentMovementRepository;
import com.silva.edu.finances.services.FinanceAccountService;
import com.silva.edu.finances.services.InvestmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class InvestmentServiceImpl implements InvestmentService {
    private final InvestmentMovementRepository repository;
    private final FinanceAccountService accountService;

    public InvestmentServiceImpl(InvestmentMovementRepository repository, FinanceAccountService accountService) {
        this.repository = repository;
        this.accountService = accountService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvestmentMovementResponse> list(String accountId) {
        accountService.requireAccess(accountId);
        return repository.findAllByUserIdAndAccountIdOrderByDateDescCreatedAtDesc(UserContext.getUser(), accountId)
                .stream().map(this::response).toList();
    }

    @Override
    public InvestmentMovementResponse create(InvestmentMovementRequest request) {
        accountService.requireAccess(request.accountId());
        InvestmentMovement movement = new InvestmentMovement();
        movement.setUserId(UserContext.getUser());
        movement.setAccountId(request.accountId().trim());
        movement.setType(request.type());
        movement.setAmount(request.amount());
        movement.setDate(request.date());
        movement.setDescription(request.description() == null ? null : request.description().trim());
        return response(repository.save(movement));
    }

    @Override
    public void delete(UUID id) {
        repository.delete(repository.findByIdAndUserId(id, UserContext.getUser())
                .orElseThrow(() -> new CustomExceptions.EntityNotFoundException("InvestmentMovement", id)));
    }

    @Override
    @Transactional(readOnly = true)
    public InvestmentSummaryResponse summary(String accountId) {
        var account = accountService.get(accountId);
        UUID userId = UserContext.getUser();
        BigDecimal contributions = repository.sumByType(userId, accountId, InvestmentMovementType.CONTRIBUTION);
        BigDecimal withdrawals = repository.sumByType(userId, accountId, InvestmentMovementType.WITHDRAWAL);
        if (contributions == null) contributions = BigDecimal.ZERO;
        if (withdrawals == null) withdrawals = BigDecimal.ZERO;
        return new InvestmentSummaryResponse(accountId, contributions, withdrawals,
                contributions.subtract(withdrawals), account.currency());
    }

    private InvestmentMovementResponse response(InvestmentMovement movement) {
        return new InvestmentMovementResponse(movement.getId(), movement.getAccountId(), movement.getType(),
                movement.getAmount(), movement.getDate(), movement.getDescription());
    }
}
