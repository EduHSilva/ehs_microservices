package com.silva.edu.finances.repositories;

import com.silva.edu.finances.domain.entities.FinanceAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface FinanceAccountRepository extends JpaRepository<FinanceAccount, String> {
    Optional<FinanceAccount> findByIdAndOwnerId(String id, UUID ownerId);
    List<FinanceAccount> findAllByOwnerId(UUID ownerId);
}
