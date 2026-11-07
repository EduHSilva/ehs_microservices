package com.edu.silva.notifications.repositories;

import com.edu.silva.notifications.domain.entities.Email;
import com.edu.silva.notifications.domain.enums.StatusEmail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmailRepository extends JpaRepository<Email, UUID> {

    long countByStatus(StatusEmail status);

    Optional<Email> findFirstByStatusOrderBySentDateDesc(StatusEmail status);
}
