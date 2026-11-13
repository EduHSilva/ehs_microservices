package com.edu.silva.users.controllers;

import com.edu.silva.common.DefaultResponse;
import com.edu.silva.common.enums.AppScope;
import com.edu.silva.users.domain.dtos.requests.AppAccessRequestDTO;
import com.edu.silva.users.domain.dtos.requests.AuthRequestDTO;
import com.edu.silva.users.domain.dtos.requests.ConfirmTokenRequestDTO;
import com.edu.silva.users.domain.dtos.requests.ForgotPasswordRequestDTO;
import com.edu.silva.users.domain.dtos.requests.RegisterRequestDTO;
import com.edu.silva.users.domain.dtos.requests.ResetPasswordRequestDTO;
import com.edu.silva.users.domain.dtos.responses.LoginResponseDTO;
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

import java.util.List;

@RestController
@RequestMapping("auth")
public class AuthController {

    /**
     * Mensagem genérica usada em fluxos públicos para não revelar se um e-mail
     * está cadastrado.
     */
    private static final String GENERIC_EMAIL_MESSAGE =
            "Se os dados informados estiverem corretos, enviaremos as instruções por e-mail";

    private final UserService service;

    public AuthController(UserService service) {
        this.service = service;
    }

    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("me")
    public ResponseEntity<DefaultResponse<LoginResponseDTO>> me(@AuthenticationPrincipal User user) {
        List<String> roles = user.getRoles().stream().map(Enum::name).toList();
        LoginResponseDTO loginResponseDTO = new LoginResponseDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getLastLogin(),
                null,
                roles,
                AppScope.appsFor(roles).stream().map(Enum::name).toList());
        return ResponseEntity.ok(new DefaultResponse<>("Auth successfully verified", loginResponseDTO));
    }

    @Operation(summary = "Register a new user")
    @ApiResponse(responseCode = "202", description = "Request accepted")
    @PostMapping("register")
    ResponseEntity<@NonNull DefaultResponse<Void>> save(@RequestBody @Valid RegisterRequestDTO dto) {
        service.save(dto);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new DefaultResponse<>(GENERIC_EMAIL_MESSAGE));
    }

    @Operation(summary = "Request access to an application for an existing account")
    @ApiResponse(responseCode = "202", description = "Request accepted")
    @PostMapping("apps/request")
    ResponseEntity<@NonNull DefaultResponse<Void>> requestAppAccess(@RequestBody @Valid AppAccessRequestDTO dto) {
        service.requestAppAccess(dto);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new DefaultResponse<>(GENERIC_EMAIL_MESSAGE));
    }

    @Operation(summary = "Confirm application access using the emailed token")
    @PostMapping("apps/confirm")
    ResponseEntity<@NonNull DefaultResponse<Void>> confirmAppAccess(@RequestBody @Valid ConfirmTokenRequestDTO dto) {
        service.confirmAppAccess(dto);
        return ResponseEntity.ok(new DefaultResponse<>(
                "Acesso à aplicação confirmado. Faça login novamente para obter um novo token"));
    }

    @Operation(summary = "Start the password reset flow")
    @ApiResponse(responseCode = "202", description = "Request accepted")
    @PostMapping("password/forgot")
    ResponseEntity<@NonNull DefaultResponse<Void>> forgotPassword(@RequestBody @Valid ForgotPasswordRequestDTO dto) {
        service.forgotPassword(dto);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new DefaultResponse<>(GENERIC_EMAIL_MESSAGE));
    }

    @Operation(summary = "Reset the password using the emailed token")
    @PostMapping("password/reset")
    ResponseEntity<@NonNull DefaultResponse<Void>> resetPassword(@RequestBody @Valid ResetPasswordRequestDTO dto) {
        service.resetPassword(dto);
        return ResponseEntity.ok(new DefaultResponse<>("Senha alterada com sucesso"));
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
