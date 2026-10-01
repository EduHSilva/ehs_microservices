package com.edu.silva.users.controllers;

import com.edu.silva.common.DefaultResponse;
import com.edu.silva.common.PageResponse;
import com.edu.silva.users.domain.dtos.requests.UpdateCompanyRequestDTO;
import com.edu.silva.users.domain.dtos.responses.CompanyResponseDTO;
import com.edu.silva.users.services.CompanyService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController()
@SecurityRequirement(name = "bearerAuth")
public class CompanyController {
    private final CompanyService service;

    private final String BASE_URL = "/companies";

    public CompanyController(CompanyService service) {
        this.service = service;
    }
    @GetMapping(BASE_URL)
    ResponseEntity<@NonNull DefaultResponse<PageResponse<CompanyResponseDTO>>> all(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<@NonNull CompanyResponseDTO> company = service.findAll(page, size);
        return ResponseEntity.ok(DefaultResponse.paged("Find all companies successfully", company, BASE_URL));
    }
    @GetMapping(BASE_URL + "/{id}")
    ResponseEntity<@NonNull DefaultResponse<CompanyResponseDTO>> one(@PathVariable UUID id) {
        return ResponseEntity.ok(new DefaultResponse<>("Company find successfully", service.findById(id), BASE_URL, id));
    }
    @PutMapping(BASE_URL + "/{id}")
    ResponseEntity<@NonNull DefaultResponse<CompanyResponseDTO>> update(@RequestBody @Valid UpdateCompanyRequestDTO request, @PathVariable UUID id) {
        return ResponseEntity.ok(new DefaultResponse<>("Company successfully updated", service.update(id, request), BASE_URL, id));
    }
}
