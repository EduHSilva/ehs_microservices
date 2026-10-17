package com.edu.silva.tasks.repositories;

import com.edu.silva.tasks.domain.entities.TaskCompletion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TaskCompletionRepository extends JpaRepository<TaskCompletion, UUID> {
    boolean existsByTaskIdAndCompletionDate(UUID taskId, LocalDate completionDate);

    long countByTaskIdAndCompletionDateBetween(UUID taskId, LocalDate startDate, LocalDate endDate);

    List<TaskCompletion> findByTaskIdAndCompletionDateBetweenOrderByCompletionDateAsc(
            UUID taskId,
            LocalDate startDate,
            LocalDate endDate
    );

    void deleteByTaskIdAndCompletionDate(UUID taskId, LocalDate completionDate);
}
