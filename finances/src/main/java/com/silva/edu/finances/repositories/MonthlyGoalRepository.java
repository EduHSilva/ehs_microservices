package com.silva.edu.finances.repositories;

import com.silva.edu.finances.domain.entities.MonthlyGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;

public interface MonthlyGoalRepository extends JpaRepository<MonthlyGoal, UUID> {
    Optional<MonthlyGoal> findByIdAndUserId(UUID id, UUID userId);
    Page<MonthlyGoal> findAllByUserIdAndAccountIdAndPeriodMonth(UUID userId, String accountId, String periodMonth, Pageable pageable);
    @Query("select g from MonthlyGoal g where g.userId = :userId and g.accountId = :accountId and ("
            + "(g.dtInicio is not null and g.dtInicio <= :periodEnd and (g.dtFim is null or g.dtFim >= :periodStart)) "
            + "or (g.dtInicio is null and g.periodMonth = :periodMonth)) "
            + "order by g.title asc")
    Page<MonthlyGoal> findApplicable(@Param("userId") UUID userId, @Param("accountId") String accountId,
            @Param("periodMonth") String periodMonth, @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd, Pageable pageable);
}
