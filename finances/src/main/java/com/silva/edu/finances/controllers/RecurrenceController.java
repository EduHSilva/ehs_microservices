package com.silva.edu.finances.controllers;

import com.edu.silva.common.DefaultResponse;
import com.edu.silva.common.PageResponse;
import com.silva.edu.finances.domain.dtos.request.AddCategoryRequestDTO;
import com.silva.edu.finances.domain.dtos.request.AddRecurrenceRequestDTO;
import com.silva.edu.finances.domain.dtos.request.UpdateCategoryRequestDTO;
import com.silva.edu.finances.domain.dtos.request.UpdateRecurrenceRequestDTO;
import com.silva.edu.finances.domain.dtos.response.CategoryResponseDTO;
import com.silva.edu.finances.domain.dtos.response.RecurrenceResponseDTO;
import com.silva.edu.finances.services.CategoryService;
import com.silva.edu.finances.services.RecurrenceService;
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
public class RecurrenceController {
    private final RecurrenceService service;

    private final String BASE_URL = "/recurrences";

    RecurrenceController(RecurrenceService service) {
        this.service = service;
    }

    @GetMapping(BASE_URL)
    ResponseEntity<@NonNull DefaultResponse<PageResponse<RecurrenceResponseDTO>>> all(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(200) int size,
            @RequestParam(required = false) String accountId
    ) {
        Page<@NonNull RecurrenceResponseDTO> t = service.findAll(page, Math.min(size, 200), accountId);
        return ResponseEntity.ok(DefaultResponse.paged("Find all recurrence successfully", t, BASE_URL));
    }

    @GetMapping(BASE_URL + "/{id}")
    ResponseEntity<@NonNull DefaultResponse<RecurrenceResponseDTO>> one(@PathVariable UUID id) {
        return ResponseEntity.ok(new DefaultResponse<>("Recurrence find successfully", service.findById(id), BASE_URL, id));
    }

    @PostMapping(BASE_URL)
    ResponseEntity<@NonNull DefaultResponse<RecurrenceResponseDTO>> save(@RequestBody @Valid AddRecurrenceRequestDTO dto) {
        RecurrenceResponseDTO t = service.save(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new DefaultResponse<>("Recurrence successfully saved", t, BASE_URL, t.getId())
        );
    }

    @PutMapping(BASE_URL + "/{id}")
    ResponseEntity<@NonNull DefaultResponse<RecurrenceResponseDTO>> replace(@RequestBody @Valid UpdateRecurrenceRequestDTO request, @PathVariable UUID id) {
        return ResponseEntity.ok(new DefaultResponse<>("Recurrence successfully updated", service.update(id, request), BASE_URL, id));
    }

    @DeleteMapping(BASE_URL + "/{id}")
    ResponseEntity<@NonNull DefaultResponse<Void>> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.ok(new DefaultResponse<Void>("Recurrence successfully deleted"));
    }
}
