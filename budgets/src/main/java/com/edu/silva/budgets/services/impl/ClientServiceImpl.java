package com.edu.silva.budgets.services.impl;

import com.edu.silva.budgets.domain.dtos.requests.NewClientRequestDTO;
import com.edu.silva.budgets.domain.dtos.requests.UpdateClientRequestDTO;
import com.edu.silva.budgets.domain.dtos.responses.ClientResponseDTO;
import com.edu.silva.budgets.domain.entities.Client;
import com.edu.silva.budgets.domain.enums.ClientStatus;
import com.edu.silva.budgets.infra.config.TenantFilter;
import com.edu.silva.budgets.infra.config.UserContext;
import com.edu.silva.budgets.infra.exceptions.CustomExceptions;
import com.edu.silva.budgets.repositories.ClientRepository;
import com.edu.silva.budgets.services.ClientService;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ClientServiceImpl implements ClientService {

    private final ClientRepository repository;
    private final TenantFilter tenantFilter;

    public ClientServiceImpl(ClientRepository userRepository, TenantFilter tenantFilter) {
        this.repository = userRepository;
        this.tenantFilter = tenantFilter;
    }

    @Transactional
    public ClientResponseDTO save(NewClientRequestDTO dto) {
        tenantFilter.apply();
        Client client = new Client(dto.name(), dto.email(), dto.phone(), dto.observations());
        client.setStatus(ClientStatus.NEW);
        client.setUserId(UserContext.getUser());
        repository.save(client);
        return new ClientResponseDTO(client);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        tenantFilter.apply();
        repository.findByIdAndUserId(id, tenantFilter.currentUserId())
                .ifPresent(entity -> repository.deleteById(id));
    }

    @Override
    public Page<@NonNull ClientResponseDTO> findAll(int page, int size, String name, String status) {
        tenantFilter.apply();
        Pageable pageable = PageRequest.of(page, size);

        if (name != null && status != null) {
            return repository
                    .findByNameContainingIgnoreCaseAndStatus(name, parseStatus(status), pageable)
                    .map(ClientResponseDTO::new);
        }

        if (name != null) {
            return repository
                    .findByNameContainingIgnoreCase(name, pageable)
                    .map(ClientResponseDTO::new);
        }

        if (status != null) {
            return repository
                    .findByStatus(parseStatus(status), pageable)
                    .map(ClientResponseDTO::new);
        }

        return repository.findAll(pageable).map(ClientResponseDTO::new);
    }

    @Override
    public ClientResponseDTO findById(UUID id) {
        tenantFilter.apply();
        return new ClientResponseDTO(repository.findByIdAndUserId(id, tenantFilter.currentUserId())
                .orElseThrow(() -> new CustomExceptions.EntityNotFoundException(Client.class.getSimpleName(), id)));
    }

    @Override
    @Transactional
    public ClientResponseDTO update(UUID id, UpdateClientRequestDTO dto) {
        tenantFilter.apply();
        return new ClientResponseDTO(repository.findByIdAndUserId(id, tenantFilter.currentUserId())
                .map(client -> {
                    client.setName(dto.name());
                    client.setEmail(dto.email());
                    client.setPhone(dto.phone());
                    client.setObservations(dto.observations());

                    if (dto.status() != null) {
                        if (dto.status() == ClientStatus.DELETED) {
                            throw new CustomExceptions.InvalidStatusException(
                                    ClientStatus.class.getSimpleName(), dto.status().name());
                        }
                        client.setStatus(dto.status());
                    }

                    return repository.save(client);
                })
                .orElseThrow(() -> new CustomExceptions.EntityNotFoundException(Client.class.getSimpleName(), id)));
    }

    private ClientStatus parseStatus(String status) {
        try {
            return ClientStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CustomExceptions.InvalidStatusException(ClientStatus.class.getSimpleName(), status);
        }
    }
}
