package com.edu.silva.users.controllers;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.edu.silva.common.DefaultResponse;
import com.edu.silva.common.enums.AppScope;
import com.edu.silva.users.domain.dtos.requests.AppAccessRequestDTO;
import com.edu.silva.users.domain.dtos.requests.AuthRequestDTO;
import com.edu.silva.users.domain.dtos.requests.ChangePasswordRequestDTO;
import com.edu.silva.users.domain.dtos.requests.ConfirmTokenRequestDTO;
import com.edu.silva.users.domain.dtos.requests.ForgotPasswordRequestDTO;
import com.edu.silva.users.domain.dtos.requests.RegisterRequestDTO;
import com.edu.silva.users.domain.dtos.requests.ResetPasswordRequestDTO;
import com.edu.silva.users.domain.dtos.responses.LoginResponseDTO;
import com.edu.silva.users.domain.entities.User;
import com.edu.silva.users.infra.exceptions.CustomExceptions;
import com.edu.silva.users.infra.security.TokenService;
import com.edu.silva.users.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("auth")
public class AuthController {

    /**
     * Mensagem genérica usada em fluxos públicos para não revelar se um e-mail
     * está cadastrado.
     */
    private static final String GENERIC_EMAIL_MESSAGE =
            "Se os dados informados estiverem corretos, enviaremos as instruções por e-mail";

    /**
     * Contas existentes não recebem e-mail no cadastro: o vínculo da nova role
     * parte do login.
     */
    private static final String REGISTER_MESSAGE =
            "Se os dados informados estiverem corretos, você receberá as instruções por e-mail. "
                    + "Se já possui conta, faça login para solicitar o novo acesso";

    private static final String BEARER_PREFIX = "Bearer ";

    private final UserService service;
    private final TokenService tokenService;

    public AuthController(UserService service, TokenService tokenService) {
        this.service = service;
        this.tokenService = tokenService;
    }

    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Current user",
            description = "Returns the authenticated user with a renewed session token "
                    + "(sliding expiration). Clients should persist the returned token and "
                    + "only discard it after a 401.")
    @GetMapping("me")
    public ResponseEntity<DefaultResponse<LoginResponseDTO>> me(
            @AuthenticationPrincipal User user,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        DecodedJWT jwt = verifyToken(authorization);
        List<String> roles = user.getRoles().stream().map(Enum::name).toList();
        LoginResponseDTO loginResponseDTO = new LoginResponseDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getLastLogin(),
                tokenService.generateToken(user, resolveApp(jwt)),
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
                .body(new DefaultResponse<>(REGISTER_MESSAGE));
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

    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Change the password of the authenticated user")
    @PutMapping("password")
    public ResponseEntity<DefaultResponse<LoginResponseDTO>> changePassword(
            @RequestBody @Valid ChangePasswordRequestDTO dto,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        DecodedJWT jwt = verifyToken(authorization);
        LoginResponseDTO response = service.changePassword(
                UUID.fromString(jwt.getSubject()), resolveApp(jwt), dto);
        return ResponseEntity.ok(new DefaultResponse<>(
                "Senha alterada com sucesso. As demais sessões foram encerradas", response));
    }

    private DecodedJWT verifyToken(String authorization) {
        DecodedJWT jwt = authorization.startsWith(BEARER_PREFIX)
                ? tokenService.verify(authorization.substring(BEARER_PREFIX.length()))
                : null;
        if (jwt == null || jwt.getSubject() == null) {
            throw new CustomExceptions.InvalidTokenException();
        }
        return jwt;
    }

    private AppScope resolveApp(DecodedJWT jwt) {
        if (jwt.getAudience() == null || jwt.getAudience().isEmpty()) {
            throw new CustomExceptions.InvalidTokenException();
        }
        return AppScope.fromName(jwt.getAudience().getFirst())
                .orElseThrow(CustomExceptions.InvalidTokenException::new);
    }

    @PostMapping("login")
    public ResponseEntity<DefaultResponse<LoginResponseDTO>> login(@RequestBody @Valid AuthRequestDTO dto) {
        Optional<LoginResponseDTO> result = service.login(dto);
        if (result.isEmpty()) {
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(new DefaultResponse<>(GENERIC_EMAIL_MESSAGE));
        }
        return ResponseEntity.ok(new DefaultResponse<>("Auth successfully verified", result.get()));
    }

    @PostMapping("logout-all")
    public ResponseEntity<DefaultResponse<Void>> logoutAll(@AuthenticationPrincipal User user) {
        service.invalidateSessions(user.getId());
        return ResponseEntity.ok(new DefaultResponse<>("Sessions successfully invalidated"));
    }
}
