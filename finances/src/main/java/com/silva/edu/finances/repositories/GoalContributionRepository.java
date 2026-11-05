package com.silva.edu.finances.repositories;

import com.silva.edu.finances.domain.entities.GoalContribution;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoalContributionRepository extends JpaRepository<GoalContribution, UUID> {
    List<GoalContribution> findAllByGoalIdAndUserIdAndDateGreaterThanEqualAndDateLessThan(
            UUID goalId, UUID userId, LocalDate start, LocalDate end);
    Optional<GoalContribution> findByIdAndUserId(UUID id, UUID userId);
    boolean existsByTransactionId(UUID transactionId);
    void deleteAllByGoalIdAndUserId(UUID goalId, UUID userId);
    @org.springframework.data.jpa.repository.Query("select coalesce(sum(c.amount), 0) from GoalContribution c where c.goal.id = :goalId and c.userId = :userId and c.date >= :start and c.date < :end")
    java.math.BigDecimal sumForGoalAndPeriod(@org.springframework.data.repository.query.Param("goalId") UUID goalId,
            @org.springframework.data.repository.query.Param("userId") UUID userId,
            @org.springframework.data.repository.query.Param("start") LocalDate start,
            @org.springframework.data.repository.query.Param("end") LocalDate end);
}
