package com.silva.edu.finances.controllers;

import com.edu.silva.common.DefaultResponse;
import com.edu.silva.common.PageResponse;
import com.silva.edu.finances.domain.dtos.request.AddCategoryRequestDTO;
import com.silva.edu.finances.domain.dtos.request.AddTransactionRequestDTO;
import com.silva.edu.finances.domain.dtos.request.UpdateCategoryRequestDTO;
import com.silva.edu.finances.domain.dtos.request.UpdateTransactionRequestDTO;
import com.silva.edu.finances.domain.dtos.response.CategoryResponseDTO;
import com.silva.edu.finances.domain.dtos.response.TransactionResponseDTO;
import com.silva.edu.finances.services.CategoryService;
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
public class CategoryController {
    private final CategoryService service;

    private final String BASE_URL = "/categories";

    CategoryController(CategoryService service) {
        this.service = service;
    }

    @GetMapping(BASE_URL)
    ResponseEntity<@NonNull DefaultResponse<PageResponse<CategoryResponseDTO>>> all(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(200) int size
    ) {
        Page<@NonNull CategoryResponseDTO> t = service.findAll(page, size);
        return ResponseEntity.ok(DefaultResponse.paged("Find all categories successfully", t, BASE_URL));
    }

    @GetMapping(BASE_URL + "/{id}")
    ResponseEntity<@NonNull DefaultResponse<CategoryResponseDTO>> one(@PathVariable UUID id) {
        return ResponseEntity.ok(new DefaultResponse<>("Category find successfully", service.findById(id), BASE_URL, id));
    }

    @PostMapping(BASE_URL)
    ResponseEntity<@NonNull DefaultResponse<CategoryResponseDTO>> save(@RequestBody @Valid AddCategoryRequestDTO dto) {
        CategoryResponseDTO t = service.save(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new DefaultResponse<>("Category successfully saved", t, BASE_URL, t.getId())
        );
    }

    @PutMapping(BASE_URL + "/{id}")
    ResponseEntity<@NonNull DefaultResponse<CategoryResponseDTO>> replace(@RequestBody @Valid UpdateCategoryRequestDTO request, @PathVariable UUID id) {
        return ResponseEntity.ok(new DefaultResponse<>("Category successfully updated", service.update(id, request), BASE_URL, id));
    }

    @DeleteMapping(BASE_URL + "/{id}")
    ResponseEntity<@NonNull DefaultResponse<Void>> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.ok(new DefaultResponse<Void>("Category successfully deleted"));
    }
}
