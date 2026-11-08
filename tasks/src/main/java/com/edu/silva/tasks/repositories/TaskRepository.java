package com.edu.silva.tasks.repositories;

import com.edu.silva.tasks.domain.entities.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;


public interface TaskRepository extends JpaRepository<Task, UUID> {
    Optional<Task> findByIdAndUserId(UUID id, UUID userId);

    @Query("""
            select t from Task t
            where t.userId = :userId
              and (t.endDate is null or t.endDate >= :start)
              and coalesce(t.startDate, t.createdDate) <= :end
            """)
    Page<Task> findAllOverlappingPeriod(
            @Param("userId") UUID userId,
            @Param("start") Date start,
            @Param("end") Date end,
            Pageable pageable);

    @Query("""
            select t from Task t
            where t.userId = :userId
              and (t.endDate is null or t.endDate >= :start)
              and coalesce(t.startDate, t.createdDate) <= :end
              and t.projectId = :projectId
            """)
    Page<Task> findAllOverlappingPeriodByProjectId(
            @Param("userId") UUID userId,
            @Param("start") Date start,
            @Param("end") Date end,
            @Param("projectId") UUID projectId,
            Pageable pageable);

    @Query("""
            select t from Task t
            where t.userId = :userId
              and (t.endDate is null or t.endDate >= :start)
              and coalesce(t.startDate, t.createdDate) <= :end
              and t.stageId = :stageId
            """)
    Page<Task> findAllOverlappingPeriodByStageId(
            @Param("userId") UUID userId,
            @Param("start") Date start,
            @Param("end") Date end,
            @Param("stageId") UUID stageId,
            Pageable pageable);
}
