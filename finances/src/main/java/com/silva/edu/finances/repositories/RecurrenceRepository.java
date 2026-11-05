package com.silva.edu.finances.repositories;

import com.silva.edu.finances.domain.entities.Recurrence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.silva.edu.finances.domain.enums.RecurrenceStatus;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecurrenceRepository extends JpaRepository<Recurrence, UUID> {
    Optional<Recurrence> findByIdAndUserId(UUID id, UUID userId);
    Page<Recurrence> findAllByAccountId(String accountId, Pageable pageable);
    List<Recurrence> findAllByAccountIdAndStatus(String accountId, RecurrenceStatus status);
    @Modifying
    @Query("update Recurrence r set r.accountId = :accountId where r.userId = :userId and r.accountId is null")
    int assignLegacyRecurrences(@Param("userId") UUID userId, @Param("accountId") String accountId);
}
