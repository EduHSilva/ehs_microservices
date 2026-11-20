package com.silva.edu.finances.domain.dtos.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreditCardInvoicePaymentResponse(UUID id, String invoiceMonth, BigDecimal value,
                                               LocalDate date, UUID transactionId) {
}
