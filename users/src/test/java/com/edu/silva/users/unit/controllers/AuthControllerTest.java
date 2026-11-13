package com.edu.silva.users.unit.controllers;

import com.edu.silva.common.enums.AppScope;
import com.edu.silva.users.controllers.AuthController;
import com.edu.silva.users.domain.dtos.requests.AuthRequestDTO;
import com.edu.silva.users.domain.dtos.requests.RegisterRequestDTO;
import com.edu.silva.users.domain.dtos.responses.LoginResponseDTO;
import com.edu.silva.users.domain.dtos.responses.UserResponseDTO;
import com.edu.silva.users.domain.entities.User;
import com.edu.silva.users.domain.enums.UserRole;
import com.edu.silva.users.infra.exceptions.CustomExceptions;
import com.edu.silva.users.infra.exceptions.GlobalExceptionsHandler;
import com.edu.silva.users.services.UserService;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private RestTestClient client;

    @Mock
    private UserService userService;

    @BeforeEach
    void setUp() {
        client = RestTestClient.bindToController(new AuthController(userService))
                .configureServer(builder -> builder.setControllerAdvice(new GlobalExceptionsHandler()))
                .defaultHeader("ContentType", "application/json")
                .build();
    }

    @Test
    void shouldLogin() {
        AuthRequestDTO request = new AuthRequestDTO("test@email.com", "password123", null, AppScope.FINANCES);
        LoginResponseDTO login = new LoginResponseDTO(
                UUID.randomUUID(), "tester", "test@email.com", new Date(), "fake-jwt-token",
                List.of("FINANCES"), List.of("FINANCES"));

        when(userService.login(any(AuthRequestDTO.class))).thenReturn(login);

        client.post()
                .uri("/auth/login")
                .body(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Auth successfully verified")
                .jsonPath("$.data.email").isEqualTo("test@email.com")
                .jsonPath("$.data.token").isEqualTo("fake-jwt-token");
    }

    @Test
    void shouldInvalidLogin() {
        AuthRequestDTO request = new AuthRequestDTO("test@email.com", "password123", null, AppScope.FINANCES);

        when(userService.login(any(AuthRequestDTO.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        client.post()
                .uri("/auth/login")
                .body(request)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Invalid credentials")
                .jsonPath("$.data").doesNotExist();
    }

    @Test
    void shouldForbidLoginWhenUserLacksAppRole() {
        AuthRequestDTO request = new AuthRequestDTO("test@email.com", "password123", null, AppScope.FINANCES);

        when(userService.login(any(AuthRequestDTO.class)))
                .thenThrow(new CustomExceptions.AppAccessDeniedException("FINANCES"));

        client.post()
                .uri("/auth/login")
                .body(request)
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Usuário não possui acesso à aplicação FINANCES");
    }

    @Test
    void shouldAcceptRegistrationWithGenericResponse() {
        RegisterRequestDTO requestDTO = new RegisterRequestDTO(
                "admin@gmail.com", RandomStringUtils.random(10),
                RandomStringUtils.random(10), UserRole.BUDGETS, null);

        client.post()
                .uri("/auth/register")
                .body(requestDTO)
                .exchange()
                .expectStatus().isAccepted()
                .expectBody()
                .jsonPath("$.message").isEqualTo(
                        "Se os dados informados estiverem corretos, enviaremos as instruções por e-mail")
                .jsonPath("$.data").doesNotExist();

        org.mockito.Mockito.verify(userService).save(any(RegisterRequestDTO.class));
    }

    @Test
    void shouldAcceptPasswordForgotWithGenericResponse() {
        client.post()
                .uri("/auth/password/forgot")
                .body(new com.edu.silva.users.domain.dtos.requests.ForgotPasswordRequestDTO("user@email.com"))
                .exchange()
                .expectStatus().isAccepted()
                .expectBody()
                .jsonPath("$.message").isEqualTo(
                        "Se os dados informados estiverem corretos, enviaremos as instruções por e-mail");

        org.mockito.Mockito.verify(userService)
                .forgotPassword(any(com.edu.silva.users.domain.dtos.requests.ForgotPasswordRequestDTO.class));
    }

    @Test
    void shouldConfirmAppAccess() {
        client.post()
                .uri("/auth/apps/confirm")
                .body(new com.edu.silva.users.domain.dtos.requests.ConfirmTokenRequestDTO("token"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo(
                        "Acesso à aplicação confirmado. Faça login novamente para obter um novo token");

        org.mockito.Mockito.verify(userService)
                .confirmAppAccess(any(com.edu.silva.users.domain.dtos.requests.ConfirmTokenRequestDTO.class));
    }
}