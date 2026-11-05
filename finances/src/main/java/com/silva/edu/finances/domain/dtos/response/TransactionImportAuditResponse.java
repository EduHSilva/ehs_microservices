package com.silva.edu.finances.domain.dtos.response;

import java.time.Instant;
import java.util.UUID;

public record TransactionImportAuditResponse(UUID id, UUID transactionId, String event, Instant createdAt) {}
