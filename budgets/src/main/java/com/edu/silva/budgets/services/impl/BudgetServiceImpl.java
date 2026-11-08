package com.edu.silva.budgets.services.impl;

import com.edu.silva.common.enums.Status;
import com.edu.silva.budgets.domain.dtos.ItemDTO;
import com.edu.silva.budgets.domain.dtos.requests.NewBudgetRequestDTO;
import com.edu.silva.budgets.domain.dtos.requests.UpdateBudgetRequestDTO;
import com.edu.silva.budgets.domain.dtos.responses.BudgetResponseDTO;
import com.edu.silva.budgets.domain.entities.Budget;
import com.edu.silva.budgets.domain.entities.Client;
import com.edu.silva.budgets.domain.entities.Item;
import com.edu.silva.budgets.domain.enums.BudgetStatus;
import com.edu.silva.budgets.infra.config.TenantFilter;
import com.edu.silva.budgets.infra.config.UserContext;
import com.edu.silva.budgets.infra.exceptions.CustomExceptions;
import com.edu.silva.budgets.repositories.BudgetRepository;
import com.edu.silva.budgets.repositories.ClientRepository;
import com.edu.silva.budgets.services.BudgetService;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class BudgetServiceImpl implements BudgetService {

    private final BudgetRepository repository;
    private final ClientRepository clientRepository;
    private final TenantFilter tenantFilter;

    public BudgetServiceImpl(BudgetRepository repository, ClientRepository clientRepository, TenantFilter tenantFilter) {
        this.repository = repository;
        this.clientRepository = clientRepository;
        this.tenantFilter = tenantFilter;
    }

    @Override
    @Transactional
    public BudgetResponseDTO save(NewBudgetRequestDTO dto) {
        tenantFilter.apply();
        UUID userId = UserContext.getUser();

        Client client = clientRepository.findByIdAndUserId(dto.client(), userId)
                .orElseThrow(() -> new CustomExceptions.EntityNotFoundException(Client.class.getSimpleName(), dto.client()));

        Budget budget = new Budget();
        budget.setStatus(BudgetStatus.DRAFT);
        budget.setUserId(userId);
        budget.setTitle(dto.title());
        budget.setDescription(dto.description());
        budget.setObservations(dto.observations());
        budget.setValidate(dto.validate());
        budget.setTerms(dto.terms());
        budget.setClient(client);

        dto.items().forEach(i -> budget.getItems().add(createItem(i, budget, userId)));
        repository.save(budget);

        return new BudgetResponseDTO(budget);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        tenantFilter.apply();
        repository.findByIdAndUserId(id, tenantFilter.currentUserId())
                .ifPresent(entity -> repository.deleteById(id));
    }

    @Override
    public Page<@NonNull BudgetResponseDTO> findAll(int page, int size, String title, String status) {
        tenantFilter.apply();
        Pageable pageable = PageRequest.of(page, size);

        if (title != null && status != null) {
            return repository
                    .findByTitleContainingIgnoreCaseAndStatus(title, parseStatus(status), pageable)
                    .map(BudgetResponseDTO::new);
        }

        if (title != null) {
            return repository
                    .findByTitleContainingIgnoreCase(title, pageable)
                    .map(BudgetResponseDTO::new);
        }

        if (status != null) {
            return repository
                    .findByStatus(parseStatus(status), pageable)
                    .map(BudgetResponseDTO::new);
        }
        return repository.findAll(pageable).map(BudgetResponseDTO::new);
    }

    @Override
    public BudgetResponseDTO findById(UUID id) {
        tenantFilter.apply();
        return new BudgetResponseDTO(repository.findByIdAndUserId(id, tenantFilter.currentUserId())
                .orElseThrow(() -> new CustomExceptions.EntityNotFoundException(Budget.class.getSimpleName(), id)));
    }

    @Override
    @Transactional
    public BudgetResponseDTO update(UUID id, UpdateBudgetRequestDTO dto) {
        tenantFilter.apply();
        UUID userId = UserContext.getUser();

        return new BudgetResponseDTO(repository.findByIdAndUserId(id, userId)
                .map(budget -> {
                    if (dto.status() != null) {
                        if (dto.status() == BudgetStatus.DELETED) {
                            throw new CustomExceptions.InvalidStatusException(
                                    BudgetStatus.class.getSimpleName(), dto.status().name());
                        }
                        budget.setStatus(dto.status());
                    }

                    budget.setTitle(dto.title());
                    budget.setDescription(dto.description());
                    budget.setValidate(dto.validate());
                    budget.setTerms(dto.terms());
                    budget.setObservations(dto.observations());

                    if (dto.kanbanOrder() != null && !dto.kanbanOrder().equals(budget.getKanbanOrder())) {
                        repository.shiftOrders(
                                budget.getStatus(),
                                dto.kanbanOrder(),
                                budget.getId(),
                                userId
                        );

                        budget.setKanbanOrder(dto.kanbanOrder());
                    }

                    Map<UUID, Item> existing = budget.getItems().stream()
                            .collect(Collectors.toMap(Item::getId, i -> i));

                    List<UUID> dtoItemIds = dto.items().stream()
                            .map(ItemDTO::getId)
                            .filter(Objects::nonNull)
                            .toList();

                    budget.getItems().removeIf(item ->
                            item.getId() != null && !dtoItemIds.contains(item.getId())
                    );

                    for (ItemDTO i : dto.items()) {
                        if (i.getId() != null && existing.containsKey(i.getId())) {
                            Item item = existing.get(i.getId());
                            item.setName(i.getName());
                            item.setDescription(i.getDescription());
                            item.setQuantity(i.getQuantity());
                            item.setPriceUn(i.getPrice());
                            item.setStatus(Status.ACTIVE);
                        } else {
                            budget.getItems().add(createItem(i, budget, userId));
                        }
                    }

                    return repository.save(budget);
                })
                .orElseThrow(() -> new CustomExceptions.EntityNotFoundException(Budget.class.getSimpleName(), id)));
    }

    private Item createItem(ItemDTO i, Budget budget, UUID userId) {
        Item item = new Item();
        item.setBudget(budget);
        item.setUserId(userId);
        item.setName(i.getName());
        item.setDescription(i.getDescription());
        item.setQuantity(i.getQuantity());
        item.setPriceUn(i.getPrice());
        item.setStatus(Status.ACTIVE);
        return item;
    }

    private BudgetStatus parseStatus(String status) {
        try {
            return BudgetStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CustomExceptions.InvalidStatusException(BudgetStatus.class.getSimpleName(), status);
        }
    }
}
