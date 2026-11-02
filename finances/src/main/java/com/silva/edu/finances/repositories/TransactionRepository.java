package com.silva.edu.finances.repositories;

import com.silva.edu.finances.domain.entities.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);
}
