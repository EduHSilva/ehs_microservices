package com.edu.silva.users.controllers;

import com.edu.silva.common.DefaultResponse;
import com.edu.silva.users.domain.dtos.requests.AuthRequestDTO;
import com.edu.silva.users.domain.dtos.requests.RegisterRequestDTO;
import com.edu.silva.users.domain.dtos.responses.LoginResponseDTO;
import com.edu.silva.users.domain.dtos.responses.UserResponseDTO;
import com.edu.silva.users.domain.entities.User;
import com.edu.silva.users.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("auth")
public class AuthController {

    private final UserService service;

    public AuthController(UserService service) {
        this.service = service;
    }
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("me")
    public ResponseEntity<DefaultResponse<LoginResponseDTO>> me(@AuthenticationPrincipal User user) {
        LoginResponseDTO loginResponseDTO = new LoginResponseDTO(user.getId(), user.getUsername(), user.getEmail(), user.getLastLogin(), null);
        return ResponseEntity.ok(new DefaultResponse<>("Auth successfully verified", loginResponseDTO));
    }

    @Operation(summary = "Register a new user")
    @ApiResponse(responseCode = "201", description = "User successfully created")
    @PostMapping("register")
    ResponseEntity<@NonNull DefaultResponse<UserResponseDTO>> save(@RequestBody @Valid RegisterRequestDTO dto) {
        UserResponseDTO u = service.save(dto);
        String BASE_URL_USERS = "/users";
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new DefaultResponse<>("User successfully saved", u, BASE_URL_USERS, u.getId())
        );
    }

    @PostMapping("login")
    public ResponseEntity<DefaultResponse<LoginResponseDTO>> login(@RequestBody @Valid AuthRequestDTO dto) {
        return ResponseEntity.ok(new DefaultResponse<>("Auth successfully verified", service.login(dto)));
    }

    @PostMapping("logout-all")
    public ResponseEntity<DefaultResponse<Void>> logoutAll(@AuthenticationPrincipal User user) {
        service.invalidateSessions(user.getId());
        return ResponseEntity.ok(new DefaultResponse<>("Sessions successfully invalidated"));
    }
}
