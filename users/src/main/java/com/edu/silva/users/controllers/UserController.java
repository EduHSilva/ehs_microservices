package com.edu.silva.users.controllers;

import com.edu.silva.common.DefaultResponse;
import com.edu.silva.common.PageResponse;
import com.edu.silva.users.domain.dtos.requests.UpdateUserRequestDTO;
import com.edu.silva.users.domain.dtos.requests.UpdateUserRolesRequestDTO;
import com.edu.silva.users.domain.dtos.requests.UpdateUserStatusRequestDTO;
import com.edu.silva.users.domain.dtos.responses.UserResponseDTO;
import com.edu.silva.users.domain.dtos.responses.UserStatsResponseDTO;
import com.edu.silva.users.domain.enums.UserRole;
import com.edu.silva.users.domain.enums.UserStatus;
import com.edu.silva.users.infra.security.annotattions.OnlyAdmin;
import com.edu.silva.users.services.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@SecurityRequirement(name = "bearerAuth")
public class UserController {
    private final UserService service;

    private final String BASE_URL = "/users";

    public UserController(UserService userService) {
        this.service = userService;
    }
    @GetMapping()
    @OnlyAdmin
    ResponseEntity<@NonNull DefaultResponse<PageResponse<UserResponseDTO>>> all(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) UserRole role,
            @RequestParam(defaultValue = "createdDate") String sort,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Page<@NonNull UserResponseDTO> users = service.search(q, status, role, page, size, sort, direction);
        return ResponseEntity.ok(DefaultResponse.paged("Find all users successfully", users, BASE_URL));
    }

    @GetMapping("/stats")
    @OnlyAdmin
    ResponseEntity<@NonNull DefaultResponse<UserStatsResponseDTO>> stats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "day") String interval
    ) {
        return ResponseEntity.ok(new DefaultResponse<>("User statistics successfully retrieved", service.stats(from, to, interval)));
    }

    @GetMapping("/export")
    @OnlyAdmin
    ResponseEntity<byte[]> export(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) UserRole role
    ) {
        byte[] csv = service.exportCsv(q, status, role).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"usuarios.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv);
    }
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == principal.id")
    ResponseEntity<@NonNull DefaultResponse<UserResponseDTO>> one(@PathVariable UUID id) {
        return ResponseEntity.ok(new DefaultResponse<>("User find successfully", service.findById(id), BASE_URL, id));
    }
    @GetMapping(value = "/confirm/{id}")
    public ResponseEntity<@NonNull DefaultResponse<Void>> confirm(@PathVariable UUID id) {
        service.confirm(id);
        // A página /user/confirm/ (estática) chama este endpoint e mostra o resultado;
        // não devolvemos dados do usuário em uma rota pública.
        return ResponseEntity.ok(new DefaultResponse<>("Conta confirmada com sucesso"));
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == principal.id")
    ResponseEntity<@NonNull DefaultResponse<UserResponseDTO>> update(@RequestBody @Valid UpdateUserRequestDTO request, @PathVariable UUID id) {
        return ResponseEntity.ok(new DefaultResponse<>("User successfully updated", service.update(id, request), BASE_URL, id));
    }
    @PatchMapping("/{id}/status")
    @OnlyAdmin
    ResponseEntity<@NonNull DefaultResponse<UserResponseDTO>> updateStatus(@RequestBody @Valid UpdateUserStatusRequestDTO request, @PathVariable UUID id) {
        return ResponseEntity.ok(new DefaultResponse<>("User status successfully updated", service.updateStatus(id, request.status()), BASE_URL, id));
    }

    @PostMapping("/{id}/roles")
    @OnlyAdmin
    ResponseEntity<@NonNull DefaultResponse<UserResponseDTO>> addRoles(
            @RequestBody @Valid UpdateUserRolesRequestDTO request,
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(new DefaultResponse<>(
                "User roles successfully updated", service.addRoles(id, request.roles()), BASE_URL, id));
    }

    @PostMapping("/{id}/resend-confirmation")
    @OnlyAdmin
    ResponseEntity<@NonNull DefaultResponse<Void>> resendConfirmation(@PathVariable UUID id) {
        service.resendConfirmation(id);
        return ResponseEntity.ok(new DefaultResponse<>("Confirmation email successfully resent"));
    }

    @DeleteMapping("/{id}")
    @OnlyAdmin
    ResponseEntity<@NonNull DefaultResponse<Void>> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.ok(new DefaultResponse<>("User successfully deleted"));
    }
}
