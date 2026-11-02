package com.silva.edu.finances.repositories;

import com.silva.edu.finances.domain.entities.Recurrence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.Optional;

public interface RecurrenceRepository extends JpaRepository<Recurrence, UUID> {
    Optional<Recurrence> findByIdAndUserId(UUID id, UUID userId);
}
