package com.silva.edu.finances.controllers;

import com.edu.silva.common.DefaultResponse;
import com.silva.edu.finances.domain.dtos.request.AddCreditCardRequest;
import com.silva.edu.finances.domain.dtos.request.InvoicePaymentRequest;
import com.silva.edu.finances.domain.dtos.request.UpdateCreditCardRequest;
import com.silva.edu.finances.domain.dtos.response.CreditCardInvoiceResponse;
import com.silva.edu.finances.domain.dtos.response.CreditCardResponse;
import com.silva.edu.finances.services.CreditCardService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@Validated
public class CreditCardController {
    private final CreditCardService service;

    private static final String BASE_URL = "/cards";

    public CreditCardController(CreditCardService service) {
        this.service = service;
    }

    @GetMapping(BASE_URL)
    ResponseEntity<@NonNull DefaultResponse<List<CreditCardResponse>>> all(@RequestParam @NotBlank String accountId) {
        return ResponseEntity.ok(new DefaultResponse<>("Credit cards found successfully", service.list(accountId)));
    }

    @GetMapping(BASE_URL + "/{cardId}")
    ResponseEntity<@NonNull DefaultResponse<CreditCardResponse>> one(@PathVariable UUID cardId) {
        return ResponseEntity.ok(new DefaultResponse<>("Credit card found successfully", service.get(cardId), BASE_URL, cardId));
    }

    @PostMapping(BASE_URL)
    ResponseEntity<@NonNull DefaultResponse<CreditCardResponse>> save(@RequestBody @Valid AddCreditCardRequest request) {
        CreditCardResponse card = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new DefaultResponse<>("Credit card successfully saved", card, BASE_URL, card.id()));
    }

    @PutMapping(BASE_URL + "/{cardId}")
    ResponseEntity<@NonNull DefaultResponse<CreditCardResponse>> replace(@PathVariable UUID cardId,
                                                                        @RequestBody @Valid UpdateCreditCardRequest request) {
        return ResponseEntity.ok(new DefaultResponse<>("Credit card successfully updated", service.update(cardId, request), BASE_URL, cardId));
    }

    @DeleteMapping(BASE_URL + "/{cardId}")
    ResponseEntity<@NonNull DefaultResponse<Void>> delete(@PathVariable UUID cardId) {
        service.delete(cardId);
        return ResponseEntity.ok(new DefaultResponse<Void>("Credit card successfully deleted"));
    }

    @GetMapping(BASE_URL + "/{cardId}/invoices")
    ResponseEntity<@NonNull DefaultResponse<List<CreditCardInvoiceResponse>>> invoices(@PathVariable UUID cardId) {
        return ResponseEntity.ok(new DefaultResponse<>("Credit card invoices found successfully", service.invoices(cardId)));
    }

    @PostMapping(BASE_URL + "/{cardId}/invoices/{invoiceMonth}/payments")
    ResponseEntity<@NonNull DefaultResponse<CreditCardInvoiceResponse>> pay(@PathVariable UUID cardId,
                                                                           @PathVariable String invoiceMonth,
                                                                           @RequestBody @Valid InvoicePaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new DefaultResponse<>(
                "Invoice payment successfully saved", service.payInvoice(cardId, invoiceMonth, request)));
    }

    @DeleteMapping(BASE_URL + "/{cardId}/invoices/{invoiceMonth}/payments/{paymentId}")
    ResponseEntity<@NonNull DefaultResponse<CreditCardInvoiceResponse>> deletePayment(@PathVariable UUID cardId,
                                                                                     @PathVariable String invoiceMonth,
                                                                                     @PathVariable UUID paymentId) {
        return ResponseEntity.ok(new DefaultResponse<>("Invoice payment successfully deleted",
                service.deletePayment(cardId, invoiceMonth, paymentId)));
    }
}
