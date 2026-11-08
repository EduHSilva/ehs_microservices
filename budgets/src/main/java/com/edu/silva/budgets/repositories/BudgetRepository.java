package com.edu.silva.budgets.repositories;

import com.edu.silva.budgets.domain.entities.Budget;
import com.edu.silva.budgets.domain.enums.BudgetStatus;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface BudgetRepository extends JpaRepository<@NonNull Budget, @NonNull UUID> {
    Page<@NonNull Budget> findByTitleContainingIgnoreCase(String name, Pageable pageable);

    Page<@NonNull Budget> findByStatus(BudgetStatus status, Pageable pageable);

    Page<@NonNull Budget> findByTitleContainingIgnoreCaseAndStatus(String name, BudgetStatus status, Pageable pageable);

    Optional<Budget> findByIdAndUserId(UUID id, UUID userId);

    List<Budget> findTop5ByOrderByCreatedDateDesc();

    List<Budget> findByStatusOrderByKanbanOrder(BudgetStatus status);

    @Modifying
    @Query("""
               UPDATE Budget b
               SET b.kanbanOrder = b.kanbanOrder + 1
               WHERE b.status = :status
                 AND b.userId = :userId
                 AND b.kanbanOrder >= :order
                 AND b.id <> :id
            """)
    void shiftOrders(
            @Param("status") BudgetStatus status,
            @Param("order") Integer order,
            @Param("id") UUID id,
            @Param("userId") UUID userId
    );
}