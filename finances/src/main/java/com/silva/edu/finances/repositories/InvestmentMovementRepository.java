package com.silva.edu.finances.repositories;

import com.silva.edu.finances.domain.entities.InvestmentMovement;
import com.silva.edu.finances.domain.enums.InvestmentMovementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvestmentMovementRepository extends JpaRepository<InvestmentMovement, UUID> {
    List<InvestmentMovement> findAllByUserIdAndAccountIdOrderByDateDescCreatedAtDesc(UUID userId, String accountId);
    Optional<InvestmentMovement> findByIdAndUserId(UUID id, UUID userId);
    @Query("select coalesce(sum(m.amount), 0) from InvestmentMovement m where m.userId = :userId and m.accountId = :accountId and m.type = :type")
    BigDecimal sumByType(@Param("userId") UUID userId, @Param("accountId") String accountId,
                         @Param("type") InvestmentMovementType type);
}
