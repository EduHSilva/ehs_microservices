package com.silva.edu.finances.controllers;

import com.edu.silva.common.DefaultResponse;
import com.edu.silva.common.PageResponse;
import com.silva.edu.finances.domain.dtos.request.AddTransactionRequestDTO;
import com.silva.edu.finances.domain.dtos.request.UpdateTransactionRequestDTO;
import com.silva.edu.finances.domain.dtos.response.TransactionResponseDTO;
import com.silva.edu.finances.domain.dtos.request.StatementImportRequest;
import com.silva.edu.finances.domain.dtos.response.StatementImportPreviewResponse;
import com.silva.edu.finances.domain.dtos.response.StatementImportResultResponse;
import com.silva.edu.finances.domain.dtos.request.ReviewImportedTransactionRequest;
import com.silva.edu.finances.domain.dtos.response.TransactionImportAuditResponse;
import com.silva.edu.finances.services.StatementImportService;
import com.silva.edu.finances.services.TransactionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

@RestController
@Validated
public class TransactionController {
    private final TransactionService service;
    private final StatementImportService importService;

    private final String BASE_URL = "/transactions";

    TransactionController(TransactionService service, StatementImportService importService) {
        this.service = service;
        this.importService = importService;
    }

    @GetMapping(BASE_URL)
    ResponseEntity<@NonNull DefaultResponse<PageResponse<TransactionResponseDTO>>> all(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(200) int size,
            @RequestParam(required = false) String accountId
    ) {
        Page<@NonNull TransactionResponseDTO> t = service.findAll(page, size, accountId);
        return ResponseEntity.ok(DefaultResponse.paged("Find all transactions successfully", t, BASE_URL));
    }

    @GetMapping(BASE_URL + "/{id}")
    ResponseEntity<@NonNull DefaultResponse<TransactionResponseDTO>> one(@PathVariable UUID id) {
        return ResponseEntity.ok(new DefaultResponse<>("Transaction find successfully", service.findById(id), BASE_URL, id));
    }

    @PostMapping(BASE_URL)
    ResponseEntity<@NonNull DefaultResponse<TransactionResponseDTO>> save(@RequestBody @Valid AddTransactionRequestDTO dto) {
        TransactionResponseDTO t = service.save(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new DefaultResponse<>("Transaction successfully saved", t, BASE_URL, t.getId())
        );
    }

    @PutMapping(BASE_URL + "/{id}")
    ResponseEntity<@NonNull DefaultResponse<TransactionResponseDTO>> replace(@RequestBody @Valid UpdateTransactionRequestDTO request, @PathVariable UUID id) {
        return ResponseEntity.ok(new DefaultResponse<>("Transaction successfully updated", service.update(id, request), BASE_URL, id));
    }

    @DeleteMapping(BASE_URL + "/{id}")
    ResponseEntity<@NonNull DefaultResponse<Void>> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.ok(new DefaultResponse<Void>("Transaction successfully deleted"));
    }

    @PostMapping(BASE_URL + "/import/preview")
    ResponseEntity<@NonNull DefaultResponse<StatementImportPreviewResponse>> previewImport(
            @RequestBody @Valid StatementImportRequest request) {
        return ResponseEntity.ok(new DefaultResponse<>("Statement preview generated", importService.preview(request)));
    }

    @PostMapping(BASE_URL + "/import/confirm")
    ResponseEntity<@NonNull DefaultResponse<StatementImportResultResponse>> confirmImport(
            @RequestBody @Valid StatementImportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new DefaultResponse<>("Statement import processed", importService.confirm(request)));
    }

    @PutMapping(BASE_URL + "/{id}/review")
    ResponseEntity<@NonNull DefaultResponse<TransactionResponseDTO>> reviewImported(
            @PathVariable UUID id, @RequestBody @Valid ReviewImportedTransactionRequest request) {
        TransactionResponseDTO result = importService.review(id, request);
        return result == null
                ? ResponseEntity.ok(new DefaultResponse<>("Imported transaction discarded"))
                : ResponseEntity.ok(new DefaultResponse<>("Imported transaction reviewed", result));
    }

    @GetMapping(BASE_URL + "/{id}/import-audit")
    ResponseEntity<@NonNull DefaultResponse<java.util.List<TransactionImportAuditResponse>>> importAudit(@PathVariable UUID id) {
        return ResponseEntity.ok(new DefaultResponse<>("Import audit found", importService.audit(id)));
    }
}
