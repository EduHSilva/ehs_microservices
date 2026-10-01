package com.silva.edu.finances.controllers;

import com.edu.silva.common.DefaultResponse;
import com.edu.silva.common.PageResponse;
import com.silva.edu.finances.domain.dtos.request.AddTransactionRequestDTO;
import com.silva.edu.finances.domain.dtos.request.UpdateTransactionRequestDTO;
import com.silva.edu.finances.domain.dtos.response.TransactionResponseDTO;
import com.silva.edu.finances.services.TransactionService;
import jakarta.validation.Valid;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class TransactionController {
    private final TransactionService service;

    private final String BASE_URL = "/transactions";

    TransactionController(TransactionService service) {
        this.service = service;
    }

    @GetMapping(BASE_URL)
    ResponseEntity<@NonNull DefaultResponse<PageResponse<TransactionResponseDTO>>> all(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<@NonNull TransactionResponseDTO> t = service.findAll(page, size);
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
}
