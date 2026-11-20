package com.silva.edu.finances.domain.dtos.response;

import java.math.BigDecimal;
import java.util.UUID;

public record CreditCardResponse(UUID id, String accountId, String name, BigDecimal cardLimit,
                                 int closingDay, int dueDay, String status, BigDecimal openInvoiceTotal) {
}
