package com.silva.edu.finances.domain.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "transaction_import_audit", indexes = @Index(name = "idx_import_audit_transaction", columnList = "transaction_id, user_id"))
@Getter @Setter @NoArgsConstructor
public class TransactionImportAudit {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "transaction_id", nullable = false, updatable = false)
    private UUID transactionId;
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;
    @Column(nullable = false, length = 16, updatable = false)
    private String event;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
