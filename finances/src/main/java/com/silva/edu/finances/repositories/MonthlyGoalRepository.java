package com.silva.edu.finances.repositories;

import com.silva.edu.finances.domain.entities.MonthlyGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MonthlyGoalRepository extends JpaRepository<MonthlyGoal, UUID> {
    Optional<MonthlyGoal> findByIdAndUserId(UUID id, UUID userId);
    Page<MonthlyGoal> findAllByUserIdAndAccountIdAndPeriodMonth(UUID userId, String accountId, String periodMonth, Pageable pageable);
}
