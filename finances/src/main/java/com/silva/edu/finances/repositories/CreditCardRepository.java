package com.silva.edu.finances.repositories;

import com.silva.edu.finances.domain.entities.CreditCard;
import com.silva.edu.finances.domain.enums.CardStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CreditCardRepository extends JpaRepository<CreditCard, UUID> {
    List<CreditCard> findAllByAccountIdOrderByNameAsc(String accountId);
    Optional<CreditCard> findByIdAndAccountId(UUID id, String accountId);
    List<CreditCard> findAllByAccountIdAndStatus(String accountId, CardStatus status);
}
