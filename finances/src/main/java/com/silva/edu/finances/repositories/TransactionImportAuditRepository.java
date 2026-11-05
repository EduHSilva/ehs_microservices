package com.silva.edu.finances.repositories;

import com.silva.edu.finances.domain.entities.TransactionImportAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TransactionImportAuditRepository extends JpaRepository<TransactionImportAudit, UUID> {
    List<TransactionImportAudit> findAllByTransactionIdAndUserIdOrderByCreatedAtAsc(UUID transactionId, UUID userId);
}
