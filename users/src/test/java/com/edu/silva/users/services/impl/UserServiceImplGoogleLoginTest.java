package com.edu.silva.users.services.impl;

import com.edu.silva.common.enums.AppScope;
import com.edu.silva.users.domain.dtos.requests.AuthRequestDTO;
import com.edu.silva.users.domain.dtos.responses.LoginResponseDTO;
import com.edu.silva.users.domain.entities.User;
import com.edu.silva.users.domain.enums.UserRole;
import com.edu.silva.users.domain.enums.UserStatus;
import com.edu.silva.users.domain.producers.UserProducer;
import com.edu.silva.users.infra.exceptions.CustomExceptions;
import com.edu.silva.users.infra.security.TokenService;
import com.edu.silva.users.repositories.CompanyRepository;
import com.edu.silva.users.repositories.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.AuthenticationManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UserServiceImplGoogleLoginTest {

    @Mock
    private UserRepository repository;
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private UserProducer producer;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private TokenService tokenService;

    private UserServiceImpl service;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        service = spy(new UserServiceImpl(
                repository, companyRepository, producer, authenticationManager, tokenService));
    }

    @Test
    void shouldProvisionStudentAccountOnFirstGoogleLogin() {
        doReturn(payload("Novo@Test.com", true, "Novo Usuário")).when(service).verifyToken("google-token");
        when(repository.findAnyByEmail("novo@test.com")).thenReturn(Optional.empty());
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenService.generateToken(any(User.class), eq(AppScope.FITNESS))).thenReturn("jwt-token");

        Optional<LoginResponseDTO> result = service.login(new AuthRequestDTO(
                null, null, "google-token", AppScope.FITNESS, UserRole.HEALTH_USER));

        assertTrue(result.isPresent());
        assertEquals("jwt-token", result.get().token());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(repository, atLeast(1)).save(captor.capture());
        User saved = captor.getAllValues().get(0);
        assertEquals("novo@test.com", saved.getEmail());
        assertEquals("Novo Usuário", saved.getUsername());
        assertEquals(UserStatus.OK, saved.getStatus());
        assertNull(saved.getPassword());
        assertEquals(List.of(UserRole.HEALTH_USER), saved.getRoles());
        verifyNoInteractions(companyRepository);
    }

    @Test
    void shouldProvisionStudentAccountWhenRoleIsNotInformed() {
        doReturn(payload("novo@test.com", true, "Novo")).when(service).verifyToken("google-token");
        when(repository.findAnyByEmail("novo@test.com")).thenReturn(Optional.empty());
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenService.generateToken(any(User.class), eq(AppScope.FITNESS))).thenReturn("jwt-token");

        service.login(new AuthRequestDTO(null, null, "google-token", AppScope.FITNESS, null));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(repository, atLeast(1)).save(captor.capture());
        assertEquals(List.of(UserRole.HEALTH_USER), captor.getAllValues().get(0).getRoles());
    }

    @Test
    void shouldOfferStudentRoleWhenLinkingFitnessWithoutRole() {
        User user = new User("Edu", "edu@test.com", "encoded", List.of(UserRole.STUDY));
        user.setId(UUID.randomUUID());
        user.setStatus(UserStatus.OK);

        doReturn(payload("edu@test.com", true, "Edu")).when(service).verifyToken("google-token");
        when(repository.findAnyByEmail("edu@test.com")).thenReturn(Optional.of(user));
        when(tokenService.generateAppAccessToken(user, AppScope.FITNESS, "HEALTH_USER")).thenReturn("token");

        Optional<LoginResponseDTO> result = service.login(new AuthRequestDTO(
                null, null, "google-token", AppScope.FITNESS, null));

        assertTrue(result.isEmpty());
        verify(producer).publishAppAccessEmail(user, "FITNESS", "HEALTH_USER", "token");
    }

    @Test
    void shouldRejectProfessionalProfileOnFirstGoogleLogin() {
        doReturn(payload("novo@test.com", true, "Novo")).when(service).verifyToken("google-token");
        when(repository.findAnyByEmail("novo@test.com")).thenReturn(Optional.empty());

        assertThrows(CustomExceptions.InvalidRoleException.class, () -> service.login(new AuthRequestDTO(
                null, null, "google-token", AppScope.FITNESS, UserRole.HEALTH_PERSONAL)));

        verify(repository, never()).save(any(User.class));
    }

    @Test
    void shouldRejectUnverifiedGoogleEmailBeforeProvisioning() {
        doReturn(payload("novo@test.com", false, "Novo")).when(service).verifyToken("google-token");

        assertThrows(CustomExceptions.InvalidGmailTokenException.class, () -> service.login(new AuthRequestDTO(
                null, null, "google-token", AppScope.FITNESS, UserRole.HEALTH_USER)));

        verify(repository, never()).findAnyByEmail(any());
        verify(repository, never()).save(any(User.class));
    }

    @Test
    void shouldNotProvisionForAdminApplication() {
        doReturn(payload("novo@test.com", true, "Novo")).when(service).verifyToken("google-token");

        assertThrows(CustomExceptions.AppAccessDeniedException.class, () -> service.login(new AuthRequestDTO(
                null, null, "google-token", AppScope.ADMIN, null)));

        verify(repository, never()).save(any(User.class));
    }

    @Test
    void shouldNotProvisionWhenEmailBelongsToDeletedAccount() {
        User deleted = new User("Edu", "edu@test.com", "encoded", List.of(UserRole.STUDY));
        deleted.setStatus(UserStatus.DELETED);

        doReturn(payload("edu@test.com", true, "Edu")).when(service).verifyToken("google-token");
        when(repository.findAnyByEmail("edu@test.com")).thenReturn(Optional.of(deleted));

        assertThrows(CustomExceptions.AccountNotActiveException.class, () -> service.login(new AuthRequestDTO(
                null, null, "google-token", AppScope.STUDY, null)));

        verify(repository, never()).save(any(User.class));
    }

    @Test
    void shouldSendAppAccessEmailWhenGoogleAccountLacksRole() {
        User user = new User("Edu", "edu@test.com", "encoded", List.of(UserRole.STUDY));
        user.setId(UUID.randomUUID());
        user.setStatus(UserStatus.OK);

        doReturn(payload("edu@test.com", true, "Edu")).when(service).verifyToken("google-token");
        when(repository.findAnyByEmail("edu@test.com")).thenReturn(Optional.of(user));
        when(tokenService.generateAppAccessToken(user, AppScope.FINANCES, "FINANCES")).thenReturn("token");

        Optional<LoginResponseDTO> result = service.login(new AuthRequestDTO(
                null, null, "google-token", AppScope.FINANCES, UserRole.FINANCES));

        assertTrue(result.isEmpty());
        verify(producer).publishAppAccessEmail(user, "FINANCES", "FINANCES", "token");
        verify(tokenService, never()).generateToken(any(User.class), any(AppScope.class));
    }

    @Test
    void shouldCreateDefaultCompanyForProvisionedBudgetsAccount() {
        doReturn(payload("novo@test.com", true, "Novo")).when(service).verifyToken("google-token");
        when(repository.findAnyByEmail("novo@test.com")).thenReturn(Optional.empty());
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(companyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenService.generateToken(any(User.class), eq(AppScope.BUDGETS))).thenReturn("jwt-token");

        service.login(new AuthRequestDTO(null, null, "google-token", AppScope.BUDGETS, UserRole.BUDGETS));

        verify(companyRepository).save(any());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(repository, atLeast(2)).save(captor.capture());
        assertEquals("Novo", captor.getAllValues().get(0).getCompany().getName());
    }

    private GoogleIdToken.Payload payload(String email, boolean verified, String name) {
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setEmail(email);
        payload.setEmailVerified(verified);
        payload.set("name", name);
        return payload;
    }
}
