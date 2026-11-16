package com.edu.silva.users.controllers;

import com.edu.silva.common.DefaultResponse;
import com.edu.silva.users.domain.dtos.responses.InternalUserProfileDTO;
import com.edu.silva.users.services.UserService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;

/**
 * Consultas de perfil entre serviços. A rota não é exposta pelo gateway
 * (o roteamento externo começa em {@code /users}); o acesso exige o segredo
 * compartilhado {@code X-Internal-Token}.
 */
@Hidden
@RestController
@RequestMapping("/internal")
public class InternalUserController {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    private final UserService service;

    @Value("${internal.token:}")
    private String internalToken;

    public InternalUserController(UserService service) {
        this.service = service;
    }

    @GetMapping("/profiles")
    public ResponseEntity<@NonNull DefaultResponse<List<InternalUserProfileDTO>>> profiles(
            @RequestParam("ids") List<UUID> ids,
            @RequestHeader(INTERNAL_TOKEN_HEADER) String token) {
        requireInternalToken(token);
        return ResponseEntity.ok(new DefaultResponse<>(
                "Profiles successfully retrieved", service.findInternalProfiles(ids)));
    }

    @GetMapping("/profiles/by-email")
    public ResponseEntity<@NonNull DefaultResponse<InternalUserProfileDTO>> profileByEmail(
            @RequestParam("email") String email,
            @RequestHeader(INTERNAL_TOKEN_HEADER) String token) {
        requireInternalToken(token);
        return service.findInternalProfileByEmail(email)
                .map(profile -> ResponseEntity.ok(new DefaultResponse<>("Profile successfully retrieved", profile)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private void requireInternalToken(String token) {
        if (internalToken == null || internalToken.isBlank() || token == null
                || !MessageDigest.isEqual(
                        internalToken.getBytes(StandardCharsets.UTF_8),
                        token.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal token");
        }
    }
}
