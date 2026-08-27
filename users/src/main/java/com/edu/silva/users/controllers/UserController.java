package com.edu.silva.users.controllers;

import com.edu.silva.common.DefaultResponse;
import com.edu.silva.users.domain.dtos.requests.UpdateUserRequestDTO;
import com.edu.silva.users.domain.dtos.responses.UserResponseDTO;
import com.edu.silva.users.services.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

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
    ResponseEntity<@NonNull DefaultResponse> all(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<@NonNull UserResponseDTO> users = service.findAll(page, size);
        return ResponseEntity.ok(new DefaultResponse("Find all users successfully", users, BASE_URL));
    }
    @GetMapping("/{id}")
    ResponseEntity<@NonNull DefaultResponse> one(@PathVariable UUID id) {
        return ResponseEntity.ok(new DefaultResponse("User find successfully", service.findById(id), BASE_URL, id));
    }
    @GetMapping(value = "/confirm/{id}")
    public RedirectView confirm(@PathVariable UUID id) {
        service.confirm(id);
        return new RedirectView("/confirm.html");
    }
    @PutMapping("/{id}")
    ResponseEntity<@NonNull DefaultResponse> update(@RequestBody @Valid UpdateUserRequestDTO request, @PathVariable UUID id) {
        return ResponseEntity.ok(new DefaultResponse("User successfully updated", service.update(id, request), BASE_URL, id));
    }
    @DeleteMapping("/{id}")
    ResponseEntity<@NonNull DefaultResponse> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.ok(new DefaultResponse("User successfully deleted"));
    }
}
