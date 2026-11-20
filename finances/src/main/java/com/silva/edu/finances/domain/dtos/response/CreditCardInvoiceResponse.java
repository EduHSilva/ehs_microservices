package com.silva.edu.finances.domain.dtos.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreditCardInvoiceResponse(UUID cardId, String invoiceMonth, LocalDate closingDate, LocalDate dueDate,
                                        BigDecimal total, BigDecimal paid, BigDecimal remaining, String status,
                                        List<CreditCardInvoicePaymentResponse> payments) {
}
