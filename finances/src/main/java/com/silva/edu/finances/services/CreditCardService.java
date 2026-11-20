package com.silva.edu.finances.services;

import com.silva.edu.finances.domain.dtos.request.AddCreditCardRequest;
import com.silva.edu.finances.domain.dtos.request.InvoicePaymentRequest;
import com.silva.edu.finances.domain.dtos.request.UpdateCreditCardRequest;
import com.silva.edu.finances.domain.dtos.response.CreditCardInvoiceResponse;
import com.silva.edu.finances.domain.dtos.response.CreditCardResponse;
import com.silva.edu.finances.domain.entities.CreditCard;

import java.util.List;
import java.util.UUID;

public interface CreditCardService {
    List<CreditCardResponse> list(String accountId);
    CreditCardResponse get(UUID cardId);
    CreditCardResponse create(AddCreditCardRequest request);
    CreditCardResponse update(UUID cardId, UpdateCreditCardRequest request);
    void delete(UUID cardId);
    List<CreditCardInvoiceResponse> invoices(UUID cardId);
    CreditCardInvoiceResponse payInvoice(UUID cardId, String invoiceMonth, InvoicePaymentRequest request);
    CreditCardInvoiceResponse deletePayment(UUID cardId, String invoiceMonth, UUID paymentId);
    /** Cartão acessível pelo usuário (404 quando não existe ou está fora do escopo). */
    CreditCard requireCard(UUID cardId);
}
