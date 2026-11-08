package com.edu.silva.budgets.repositories;

import com.edu.silva.budgets.domain.entities.Client;
import com.edu.silva.budgets.domain.enums.ClientStatus;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;


public interface ClientRepository extends JpaRepository<@NonNull Client, @NonNull UUID> {
    Page<@NonNull Client> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<@NonNull Client> findByStatus(ClientStatus status, Pageable pageable);

    Page<@NonNull Client> findByNameContainingIgnoreCaseAndStatus(String name, ClientStatus status, Pageable pageable);

    Optional<Client> findByIdAndUserId(UUID id, UUID userId);

}