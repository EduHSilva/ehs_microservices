package com.edu.silva.budgets.controllers;

import com.edu.silva.common.DefaultResponse;
import com.edu.silva.common.PageResponse;
import com.edu.silva.budgets.domain.dtos.requests.NewClientRequestDTO;
import com.edu.silva.budgets.domain.dtos.requests.UpdateClientRequestDTO;
import com.edu.silva.budgets.domain.dtos.responses.ClientResponseDTO;
import com.edu.silva.budgets.services.ClientService;
import jakarta.validation.Valid;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class ClientController {

    private final String BASE_URL = "/budgets/clients";

    private final ClientService service;

    public ClientController(ClientService clientService) {
        this.service = clientService;
    }

    @GetMapping("/clients")
    public ResponseEntity<@NonNull DefaultResponse<PageResponse<ClientResponseDTO>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status
    ) {
        Page<@NonNull ClientResponseDTO> clients = service.findAll(page, size, name, status);

        return ResponseEntity.ok(
                DefaultResponse.paged("Find all clients successfully", clients, BASE_URL)
        );
    }

    @PostMapping("/clients")
    public ResponseEntity<@NonNull DefaultResponse<ClientResponseDTO>> add(
            @RequestBody @Valid NewClientRequestDTO dto
    ) {
        ClientResponseDTO client = service.save(dto);

        return ResponseEntity.ok(
                new DefaultResponse<>("Client successfully saved", client, BASE_URL, client.getId())
        );
    }

    @GetMapping("/clients/{id}")
    public ResponseEntity<@NonNull DefaultResponse<ClientResponseDTO>> get(
            @PathVariable UUID id
    ) {
        ClientResponseDTO dto = service.findById(id);
        return ResponseEntity.ok(
                new DefaultResponse<>("Client successfully retrieved", dto, BASE_URL, id)
        );
    }

    @DeleteMapping("/clients/{id}")
    public ResponseEntity<@NonNull DefaultResponse<Void>> delete(
            @PathVariable UUID id
    ) {
        service.delete(id);
        return ResponseEntity.ok(
                new DefaultResponse<Void>("Client successfully deleted")
        );
    }

    @PutMapping("/clients/{id}")
    public ResponseEntity<@NonNull DefaultResponse<ClientResponseDTO>> update(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateClientRequestDTO dto
    ) {
        return ResponseEntity.ok(
                new DefaultResponse<>("Client successfully updated", service.update(id, dto), BASE_URL, id)
        );
    }
}
