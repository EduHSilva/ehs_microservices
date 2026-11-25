package com.edu.silva.users.unit.controllers;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.edu.silva.common.enums.AppScope;
import com.edu.silva.users.controllers.AuthController;
import com.edu.silva.users.domain.dtos.requests.AuthRequestDTO;
import com.edu.silva.users.domain.dtos.requests.ChangePasswordRequestDTO;
import com.edu.silva.users.domain.dtos.requests.RegisterRequestDTO;
import com.edu.silva.users.domain.dtos.responses.LoginResponseDTO;
import com.edu.silva.users.domain.dtos.responses.UserResponseDTO;
import com.edu.silva.users.domain.entities.User;
import com.edu.silva.users.domain.enums.UserRole;
import com.edu.silva.users.infra.exceptions.CustomExceptions;
import com.edu.silva.users.infra.exceptions.GlobalExceptionsHandler;
import com.edu.silva.users.infra.security.TokenService;
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
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private RestTestClient client;

    @Mock
    private UserService userService;

    @Mock
    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        client = RestTestClient.bindToController(new AuthController(userService, tokenService))
                .configureServer(builder -> builder.setControllerAdvice(new GlobalExceptionsHandler()))
                .defaultHeader("ContentType", "application/json")
                .build();
    }

    @Test
    void shouldLogin() {
        AuthRequestDTO request = new AuthRequestDTO("test@email.com", "password123", null, AppScope.FINANCES, null);
        LoginResponseDTO login = new LoginResponseDTO(
                UUID.randomUUID(), "tester", "test@email.com", new Date(), "fake-jwt-token",
                List.of("FINANCES"), List.of("FINANCES"));

        when(userService.login(any(AuthRequestDTO.class))).thenReturn(Optional.of(login));

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
        AuthRequestDTO request = new AuthRequestDTO("test@email.com", "password123", null, AppScope.FINANCES, null);

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
    void shouldAcceptLoginWhenAppAccessLinkWasSent() {
        AuthRequestDTO request = new AuthRequestDTO("test@email.com", "password123", null, AppScope.FINANCES, UserRole.FINANCES);

        when(userService.login(any(AuthRequestDTO.class))).thenReturn(Optional.empty());

        client.post()
                .uri("/auth/login")
                .body(request)
                .exchange()
                .expectStatus().isAccepted()
                .expectBody()
                .jsonPath("$.message").isEqualTo(
                        "Se os dados informados estiverem corretos, enviaremos as instruções por e-mail")
                .jsonPath("$.data").doesNotExist();
    }

    @Test
    void shouldForbidLoginToAdminApp() {
        AuthRequestDTO request = new AuthRequestDTO("test@email.com", "password123", null, AppScope.ADMIN, null);

        when(userService.login(any(AuthRequestDTO.class)))
                .thenThrow(new CustomExceptions.AppAccessDeniedException("ADMIN"));

        client.post()
                .uri("/auth/login")
                .body(request)
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Usuário não possui acesso à aplicação ADMIN");
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
                        "Se os dados informados estiverem corretos, você receberá as instruções por e-mail. "
                                + "Se já possui conta, faça login para solicitar o novo acesso")
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
    void shouldChangePasswordReturningFreshToken() {
        UUID id = UUID.randomUUID();
        DecodedJWT jwt = mock(DecodedJWT.class);
        when(jwt.getSubject()).thenReturn(id.toString());
        when(jwt.getAudience()).thenReturn(List.of("ADMIN"));
        when(tokenService.verify("current-token")).thenReturn(jwt);
        when(userService.changePassword(any(UUID.class), any(AppScope.class), any(ChangePasswordRequestDTO.class)))
                .thenReturn(new LoginResponseDTO(id, "tester", "test@email.com", null, "fresh-token",
                        List.of("ADMIN"), List.of("ADMIN")));

        client.put()
                .uri("/auth/password")
                .header("Authorization", "Bearer current-token")
                .body(new ChangePasswordRequestDTO("senhaAtual1", "novaSenha123"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.token").isEqualTo("fresh-token");

        org.mockito.Mockito.verify(userService).changePassword(
                id, AppScope.ADMIN, new ChangePasswordRequestDTO("senhaAtual1", "novaSenha123"));
    }

    @Test
    void shouldReturnBadRequestWhenCurrentPasswordIsWrong() {
        UUID id = UUID.randomUUID();
        DecodedJWT jwt = mock(DecodedJWT.class);
        when(jwt.getSubject()).thenReturn(id.toString());
        when(jwt.getAudience()).thenReturn(List.of("ADMIN"));
        when(tokenService.verify("current-token")).thenReturn(jwt);
        when(userService.changePassword(any(UUID.class), any(AppScope.class), any(ChangePasswordRequestDTO.class)))
                .thenThrow(new CustomExceptions.InvalidPasswordException("Senha atual incorreta"));

        client.put()
                .uri("/auth/password")
                .header("Authorization", "Bearer current-token")
                .body(new ChangePasswordRequestDTO("errada", "novaSenha123"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Senha atual incorreta");
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