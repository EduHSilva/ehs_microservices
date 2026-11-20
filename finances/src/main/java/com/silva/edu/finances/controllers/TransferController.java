package com.silva.edu.finances.controllers;

import com.edu.silva.common.DefaultResponse;
import com.silva.edu.finances.domain.dtos.request.TransferRequest;
import com.silva.edu.finances.domain.dtos.request.UpdateTransferRequest;
import com.silva.edu.finances.domain.dtos.response.TransferResponse;
import com.silva.edu.finances.services.TransferService;
import jakarta.validation.Valid;
import lombok.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class TransferController {
    private final TransferService service;

    private static final String BASE_URL = "/transfers";

    public TransferController(TransferService service) {
        this.service = service;
    }

    @PostMapping(BASE_URL)
    ResponseEntity<@NonNull DefaultResponse<TransferResponse>> save(@RequestBody @Valid TransferRequest request) {
        TransferResponse transfer = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new DefaultResponse<>(
                "Transfer successfully saved", transfer, BASE_URL, transfer.transferId()));
    }

    @GetMapping(BASE_URL + "/{transferId}")
    ResponseEntity<@NonNull DefaultResponse<TransferResponse>> one(@PathVariable UUID transferId) {
        return ResponseEntity.ok(new DefaultResponse<>("Transfer found successfully",
                service.find(transferId), BASE_URL, transferId));
    }

    @PutMapping(BASE_URL + "/{transferId}")
    ResponseEntity<@NonNull DefaultResponse<TransferResponse>> replace(@PathVariable UUID transferId,
                                                                      @RequestBody @Valid UpdateTransferRequest request) {
        return ResponseEntity.ok(new DefaultResponse<>("Transfer successfully updated",
                service.update(transferId, request), BASE_URL, transferId));
    }

    @DeleteMapping(BASE_URL + "/{transferId}")
    ResponseEntity<@NonNull DefaultResponse<Void>> delete(@PathVariable UUID transferId) {
        service.delete(transferId);
        return ResponseEntity.ok(new DefaultResponse<Void>("Transfer successfully deleted"));
    }
}
