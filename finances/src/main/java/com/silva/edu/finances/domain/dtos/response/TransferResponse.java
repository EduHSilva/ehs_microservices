package com.silva.edu.finances.domain.dtos.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TransferResponse(UUID transferId, String fromAccountId, String toAccountId, BigDecimal value,
                               LocalDate date, String title, String status, UUID outTransactionId,
                               UUID inTransactionId) {
}
