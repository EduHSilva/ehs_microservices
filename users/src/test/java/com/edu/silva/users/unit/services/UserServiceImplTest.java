package com.edu.silva.users.unit.services;

import com.edu.silva.common.enums.AppScope;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.edu.silva.users.domain.dtos.requests.AppAccessRequestDTO;
import com.edu.silva.users.domain.dtos.requests.AuthRequestDTO;
import com.edu.silva.users.domain.dtos.requests.ConfirmTokenRequestDTO;
import com.edu.silva.users.domain.dtos.requests.ForgotPasswordRequestDTO;
import com.edu.silva.users.domain.dtos.requests.RegisterRequestDTO;
import com.edu.silva.users.domain.dtos.requests.ResetPasswordRequestDTO;
import com.edu.silva.users.domain.dtos.requests.UpdateUserRequestDTO;
import com.edu.silva.users.domain.dtos.responses.LoginResponseDTO;
import com.edu.silva.users.domain.dtos.responses.UserResponseDTO;
import com.edu.silva.users.domain.dtos.responses.UserStatsResponseDTO;
import com.edu.silva.users.domain.entities.Company;
import com.edu.silva.users.domain.entities.User;
import com.edu.silva.users.domain.enums.UserRole;
import com.edu.silva.users.domain.enums.UserStatus;
import com.edu.silva.users.domain.producers.UserProducer;
import com.edu.silva.users.infra.exceptions.CustomExceptions;
import com.edu.silva.users.infra.security.TokenService;
import com.edu.silva.users.repositories.CompanyRepository;
import com.edu.silva.users.repositories.UserRepository;
import com.edu.silva.users.services.impl.UserServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class UserServiceImplTest {

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

    @InjectMocks
    private UserServiceImpl service;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAsAdmin() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                )
        );
    }

    private void authenticateAsUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "user",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                )
        );
    }

    @Test
    void shouldSaveUserSuccessfully() {
        RegisterRequestDTO dto = new RegisterRequestDTO(
                "Edu@test.com", "Eduardo", "123", UserRole.BUDGETS, null
        );

        when(repository.findAnyByEmail("edu@test.com")).thenReturn(Optional.empty());

        service.save(dto);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(repository).save(captor.capture());
        User saved = captor.getValue();

        assertEquals("Eduardo", saved.getUsername());
        assertEquals("edu@test.com", saved.getEmail());
        assertEquals(UserStatus.WAITING_CONFIRMATION, saved.getStatus());

        verify(producer, times(1)).publishMessageEmail(any(User.class));
    }

    @Test
    void shouldSaveUserSuccessfullyWithCompany() {
        UUID companyID = UUID.randomUUID();
        Company company = new Company();
        company.setId(companyID);
        RegisterRequestDTO dto = new RegisterRequestDTO(
                "edu@test.com", "Eduardo", "123", UserRole.BUDGETS, companyID
        );

        when(repository.findAnyByEmail("edu@test.com")).thenReturn(Optional.empty());
        when(companyRepository.findById(companyID)).thenReturn(Optional.of(company));

        service.save(dto);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(repository).save(captor.capture());
        User saved = captor.getValue();

        assertEquals("Eduardo", saved.getUsername());
        assertEquals(UserStatus.WAITING_CONFIRMATION, saved.getStatus());
        assertEquals(company, saved.getCompany());

        verify(producer, times(1)).publishMessageEmail(any(User.class));
    }

    @Test
    void shouldThrowWhenCompanyNotFound() {
        UUID companyID = UUID.randomUUID();
        RegisterRequestDTO dto = new RegisterRequestDTO(
                "edu@test.com", "Eduardo", "123", UserRole.BUDGETS, companyID
        );

        when(repository.findAnyByEmail("edu@test.com")).thenReturn(Optional.empty());
        when(companyRepository.findById(companyID)).thenReturn(Optional.empty());

        assertThrows(CustomExceptions.EntityNotFoundException.class, () -> service.save(dto));
        verify(repository, never()).save(any());
    }

    @Test
    void shouldLinkAppAccessWithoutCreatingDuplicateAccount() {
        User existing = new User("Edu", "edu@test.com", "encoded", new java.util.ArrayList<>(List.of(UserRole.STUDY)));
        existing.setId(UUID.randomUUID());
        existing.setStatus(UserStatus.OK);

        RegisterRequestDTO dto = new RegisterRequestDTO(
                "EDU@test.com", "Eduardo", "outra-senha", UserRole.FINANCES, null
        );

        when(repository.findAnyByEmail("edu@test.com")).thenReturn(Optional.of(existing));
        when(tokenService.generateAppAccessToken(existing, AppScope.FINANCES, "FINANCES")).thenReturn("token");

        service.save(dto);

        verify(repository, never()).save(any());
        verify(producer).publishAppAccessEmail(existing, "FINANCES", "FINANCES", "token");

        // A senha original nunca é alterada.
        assertEquals("encoded", existing.getPassword());
        assertEquals(List.of(UserRole.STUDY), existing.getRoles());
    }

    @Test
    void shouldNotCreateDuplicateNorSendEmailForBlockedUser() {
        User existing = new User("Edu", "edu@test.com", "encoded", List.of(UserRole.STUDY));
        existing.setStatus(UserStatus.BLOCKED);

        when(repository.findAnyByEmail("edu@test.com")).thenReturn(Optional.of(existing));

        service.save(new RegisterRequestDTO("edu@test.com", "Edu", "123", UserRole.FINANCES, null));

        verify(repository, never()).save(any());
        verify(producer, never()).publishAppAccessEmail(any(), any(), any(), any());
        verify(producer, never()).publishMessageEmail(any());
    }

    @Test
    void shouldNotCreateDuplicateNorSendEmailForDeletedUser() {
        User existing = new User("Edu", "edu@test.com", "encoded", List.of(UserRole.STUDY));
        existing.setStatus(UserStatus.DELETED);

        when(repository.findAnyByEmail("edu@test.com")).thenReturn(Optional.of(existing));

        service.save(new RegisterRequestDTO("edu@test.com", "Edu", "123", UserRole.FINANCES, null));

        verify(repository, never()).save(any());
        verify(producer, never()).publishAppAccessEmail(any(), any(), any(), any());
        verify(producer, never()).publishMessageEmail(any());
    }

    @Test
    void shouldThrowWhenUserRegistersAsAdmin() {
        RegisterRequestDTO dto = new RegisterRequestDTO(
                "Eduardo", "edu@test.com", "123", UserRole.ADMIN, null
        );

        assertThrows(CustomExceptions.InvalidRoleException.class, () -> service.save(dto));
        verify(repository, never()).save(any());
    }

    @Test
    void shouldReturnUserWhenFound() {
        UUID id = UUID.randomUUID();
        User user = new User("Eduardo", "edu@test.com", "123", Collections.singletonList(UserRole.BUDGETS));
        Company company = new Company();
        company.setName("test");
        user.setCompany(company);
        when(repository.findById(id)).thenReturn(Optional.of(user));

        UserResponseDTO result = service.findById(id);
        assertEquals("Eduardo", result.getUsername());
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(CustomExceptions.EntityNotFoundException.class, () -> service.findById(id));
    }

    @Test
    void shouldUpdateUser() {
        UUID id = UUID.randomUUID();

        User user = new User("Old", "old@test.com", "123", Collections.singletonList(UserRole.BUDGETS));
        Company company = new Company();
        company.setName("Teste");
        user.setCompany(company);
        UpdateUserRequestDTO req = new UpdateUserRequestDTO("NewName");

        when(repository.findById(id)).thenReturn(Optional.of(user));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponseDTO updated = service.update(id, req);

        assertEquals("NewName", updated.getUsername());
    }

    @Test
    void shouldThrowWhenUpdatingNonExistingUser() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        UpdateUserRequestDTO req = new UpdateUserRequestDTO("Test");

        assertThrows(CustomExceptions.EntityNotFoundException.class, () -> service.update(id, req));
    }

    @Test
    void shouldUpdateUserEmailAndRolesAsAdmin() {
        authenticateAsAdmin();
        UUID id = UUID.randomUUID();
        User user = new User("Old", "old@test.com", "123", Collections.singletonList(UserRole.BUDGETS));
        Company company = new Company();
        company.setName("Teste");
        user.setCompany(company);

        UpdateUserRequestDTO req = new UpdateUserRequestDTO(
                "NewName",
                "New@test.com",
                List.of(UserRole.BUDGETS, UserRole.STUDY)
        );

        when(repository.findById(id)).thenReturn(Optional.of(user));
        when(repository.findAnyByEmail("new@test.com")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponseDTO updated = service.update(id, req);

        assertEquals("NewName", updated.getUsername());
        assertEquals("new@test.com", updated.getEmail());
        assertEquals(List.of(UserRole.BUDGETS, UserRole.STUDY), updated.getRoles());
        // Roles mudaram: força novo login.
        assertEquals(1, user.getTokenVersion());
    }

    @Test
    void shouldNotForceReloginWhenRolesDoNotChange() {
        authenticateAsAdmin();
        UUID id = UUID.randomUUID();
        User user = new User("Old", "old@test.com", "123", List.of(UserRole.BUDGETS, UserRole.STUDY));

        UpdateUserRequestDTO req = new UpdateUserRequestDTO(
                "Old", null, List.of(UserRole.STUDY, UserRole.BUDGETS));

        when(repository.findById(id)).thenReturn(Optional.of(user));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.update(id, req);

        assertEquals(0, user.getTokenVersion());
    }

    @Test
    void shouldRejectAddingAdminRoleEvenForAdmin() {
        authenticateAsAdmin();
        UUID id = UUID.randomUUID();
        User user = new User("Old", "old@test.com", "123", Collections.singletonList(UserRole.BUDGETS));

        UpdateUserRequestDTO req = new UpdateUserRequestDTO(
                "Old", null, List.of(UserRole.BUDGETS, UserRole.ADMIN));

        when(repository.findById(id)).thenReturn(Optional.of(user));

        assertThrows(CustomExceptions.InvalidRoleException.class, () -> service.update(id, req));
        verify(repository, never()).save(any());
    }

    @Test
    void shouldRejectRemovingAdminRole() {
        authenticateAsAdmin();
        UUID id = UUID.randomUUID();
        User user = new User("Old", "old@test.com", "123", List.of(UserRole.ADMIN, UserRole.BUDGETS));

        UpdateUserRequestDTO req = new UpdateUserRequestDTO("Old", null, List.of(UserRole.BUDGETS));

        when(repository.findById(id)).thenReturn(Optional.of(user));

        assertThrows(CustomExceptions.InvalidRoleException.class, () -> service.update(id, req));
        verify(repository, never()).save(any());
    }

    @Test
    void shouldBlockNonAdminFromChangingRoles() {
        authenticateAsUser();
        UUID id = UUID.randomUUID();
        User user = new User("Old", "old@test.com", "123", Collections.singletonList(UserRole.BUDGETS));

        UpdateUserRequestDTO req = new UpdateUserRequestDTO(
                "Old",
                null,
                List.of(UserRole.BUDGETS, UserRole.STUDY)
        );

        when(repository.findById(id)).thenReturn(Optional.of(user));

        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> service.update(id, req));
        verify(repository, never()).save(any());
    }

    @Test
    void shouldBlockUserFromAssigningAdminRole() {
        authenticateAsUser();
        UUID id = UUID.randomUUID();
        User user = new User("Old", "old@test.com", "123", Collections.singletonList(UserRole.BUDGETS));

        UpdateUserRequestDTO req = new UpdateUserRequestDTO(
                "Old",
                null,
                List.of(UserRole.ADMIN, UserRole.BUDGETS)
        );

        when(repository.findById(id)).thenReturn(Optional.of(user));

        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> service.update(id, req));
        verify(repository, never()).save(any());
    }

    @Test
    void shouldThrowWhenUpdatingWithEmailFromAnotherUser() {
        authenticateAsUser();
        UUID id = UUID.randomUUID();
        User user = new User("Old", "old@test.com", "123", Collections.singletonList(UserRole.BUDGETS));
        User other = new User();
        other.setId(UUID.randomUUID());

        UpdateUserRequestDTO req = new UpdateUserRequestDTO("NewName", "used@test.com", null);

        when(repository.findById(id)).thenReturn(Optional.of(user));
        when(repository.findAnyByEmail("used@test.com")).thenReturn(Optional.of(other));

        assertThrows(CustomExceptions.EmailAlreadyExistsException.class, () -> service.update(id, req));
    }

    @Test
    void shouldConfirmUserSuccessfully() {
        UUID id = UUID.randomUUID();
        User user = new User("Edu", "edu@test.com", "123", Collections.singletonList(UserRole.BUDGETS));
        user.setStatus(UserStatus.WAITING_CONFIRMATION);
        Company company = new Company();
        company.setName("test");
        user.setCompany(company);
        when(repository.findById(id)).thenReturn(Optional.of(user));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponseDTO confirmed = service.confirm(id);

        assertEquals(UserStatus.OK, confirmed.getStatus());
    }

    @Test
    void shouldNotConfirmWhenStatusInvalid() {
        UUID id = UUID.randomUUID();
        User user = new User("Edu", "edu@test.com", "123", Collections.singletonList(UserRole.BUDGETS));
        user.setStatus(UserStatus.OK);

        when(repository.findById(id)).thenReturn(Optional.of(user));

        assertThrows(CustomExceptions.InvalidStatusException.class, () -> service.confirm(id));
    }

    @Test
    void shouldReleaseUserBySettingStatusOk() {
        UUID id = UUID.randomUUID();
        User user = new User("Edu", "edu@test.com", "123", Collections.singletonList(UserRole.BUDGETS));
        user.setStatus(UserStatus.WAITING_CONFIRMATION);

        when(repository.findById(id)).thenReturn(Optional.of(user));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponseDTO result = service.updateStatus(id, UserStatus.OK);

        assertEquals(UserStatus.OK, result.getStatus());
        assertEquals(0, user.getTokenVersion());
    }

    @Test
    void shouldBlockUserAndInvalidateSessions() {
        UUID id = UUID.randomUUID();
        User user = new User("Edu", "edu@test.com", "123", Collections.singletonList(UserRole.BUDGETS));
        user.setStatus(UserStatus.OK);
        user.setTokenVersion(2);

        when(repository.findById(id)).thenReturn(Optional.of(user));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponseDTO result = service.updateStatus(id, UserStatus.BLOCKED);

        assertEquals(UserStatus.BLOCKED, result.getStatus());
        assertEquals(3, user.getTokenVersion());
    }

    @Test
    void shouldRejectStatusOutsideOkAndBlocked() {
        UUID id = UUID.randomUUID();
        User user = new User("Edu", "edu@test.com", "123", Collections.singletonList(UserRole.BUDGETS));
        user.setStatus(UserStatus.OK);

        when(repository.findById(id)).thenReturn(Optional.of(user));

        assertThrows(CustomExceptions.InvalidStatusException.class,
                () -> service.updateStatus(id, UserStatus.DELETED));
        verify(repository, never()).save(any());
    }

    @Test
    void shouldResendConfirmationWhenWaitingConfirmation() {
        UUID id = UUID.randomUUID();
        User user = new User("Edu", "edu@test.com", "123", Collections.singletonList(UserRole.BUDGETS));
        user.setStatus(UserStatus.WAITING_CONFIRMATION);

        when(repository.findById(id)).thenReturn(Optional.of(user));

        service.resendConfirmation(id);

        verify(producer).publishMessageEmail(user);
    }

    @Test
    void shouldRejectResendWhenStatusIsNotWaitingConfirmation() {
        UUID id = UUID.randomUUID();
        User user = new User("Edu", "edu@test.com", "123", Collections.singletonList(UserRole.BUDGETS));
        user.setStatus(UserStatus.OK);

        when(repository.findById(id)).thenReturn(Optional.of(user));

        assertThrows(CustomExceptions.InvalidStatusException.class, () -> service.resendConfirmation(id));
        verify(producer, never()).publishMessageEmail(any());
    }

    @Test
    void shouldRejectResendWithinCooldown() {
        UUID id = UUID.randomUUID();
        User user = new User("Edu", "edu@test.com", "123", Collections.singletonList(UserRole.BUDGETS));
        user.setStatus(UserStatus.WAITING_CONFIRMATION);

        when(repository.findById(id)).thenReturn(Optional.of(user));

        service.resendConfirmation(id);
        assertThrows(CustomExceptions.InvalidStatusException.class, () -> service.resendConfirmation(id));
        verify(producer, times(1)).publishMessageEmail(user);
    }

    @Test
    void shouldInvalidateSessionsIncrementingTokenVersion() {
        UUID id = UUID.randomUUID();
        User user = new User("Edu", "edu@test.com", "123", Collections.singletonList(UserRole.BUDGETS));
        user.setTokenVersion(5);

        when(repository.findById(id)).thenReturn(Optional.of(user));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.invalidateSessions(id);

        assertEquals(6, user.getTokenVersion());
        verify(repository).save(user);
    }

    @Test
    void shouldSoftDeleteUserById() {
        UUID id = UUID.randomUUID();
        User user = new User("Old", "old@test.com", "123", Collections.singletonList(UserRole.BUDGETS));
        user.setStatus(UserStatus.OK);

        when(repository.findById(id)).thenReturn(Optional.of(user));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.delete(id);

        assertEquals(UserStatus.DELETED, user.getStatus());
        verify(repository).save(user);
        verify(repository, never()).deleteById(any());
    }

    @Test
    void shouldSearchUsersWithFilters() {
        User user = new User("Edu", "edu@test.com", "123", Collections.singletonList(UserRole.BUDGETS));
        Company company = new Company();
        company.setName("Test");
        user.setCompany(company);

        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user)));

        Page<UserResponseDTO> result = service.search("edu", UserStatus.OK, UserRole.BUDGETS, 0, 10, "username", "asc");

        assertEquals(1, result.getContent().size());
        assertEquals("Edu", result.getContent().get(0).getUsername());
    }

    @Test
    void shouldExportUsersCsv() {
        User user = new User("Edu", "edu@test.com", "123", Collections.singletonList(UserRole.BUDGETS));
        Company company = new Company();
        company.setName("Test");
        user.setCompany(company);

        when(repository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(user));

        String csv = service.exportCsv(null, null, null);

        assertTrue(csv.contains("username"));
        assertTrue(csv.contains("edu@test.com"));
    }

    @Test
    void shouldReturnStatsForPeriod() {
        LocalDate end = LocalDate.of(2026, 11, 6);
        LocalDate start = end.minusDays(2);

        when(repository.findAllCreatedDates()).thenReturn(List.of(toDate(start), toDate(end)));
        when(companyRepository.findAllCreatedDates()).thenReturn(List.of(toDate(end)));
        when(repository.count()).thenReturn(10L);
        when(companyRepository.count()).thenReturn(3L);
        when(repository.countByStatus(UserStatus.WAITING_CONFIRMATION)).thenReturn(2L);
        when(repository.countByStatus(UserStatus.BLOCKED)).thenReturn(1L);
        when(repository.countByCompanyIsNull()).thenReturn(4L);

        UserStatsResponseDTO stats = service.stats(start, end, "day");

        assertEquals(10, stats.totalUsers());
        assertEquals(3, stats.totalCompanies());
        assertEquals(2, stats.waitingConfirmation());
        assertEquals(1, stats.blockedUsers());
        assertEquals(4, stats.usersWithoutCompany());
        assertEquals(3, stats.series().size());
        assertEquals(1, stats.series().get(0).users());
        assertEquals(1, stats.series().get(2).users());
        assertEquals(1, stats.series().get(2).companies());
    }

    @Test
    void shouldRejectInvalidInterval() {
        assertThrows(CustomExceptions.InvalidStatusException.class,
                () -> service.stats(LocalDate.now(), LocalDate.now(), "year"));
    }

    private Date toDate(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    @Test
    void shouldReturnPagedUsers() {
        int page = 0;
        int size = 2;

        Pageable pageable = PageRequest.of(page, size);

        User user1 = new User();
        User user2 = new User();

        Company company = new Company();
        company.setName("test");
        user1.setCompany(company);
        user2.setCompany(company);
        List<User> users = List.of(user1, user2);

        Page<User> userPage = new PageImpl<>(users, pageable, users.size());

        when(repository.findAll(pageable)).thenReturn(userPage);

        User user = new User();
        user.getRoles().add(UserRole.ADMIN);
        Page<UserResponseDTO> result = service.findAll(page, size);

        assertEquals(2, result.getContent().size());
        verify(repository).findAll(pageable);
    }

    @Test
    void shouldLoginWhenUserHasRoleForRequestedApp() {
        User user = new User("Edu", "edu@test.com", "encoded", List.of(UserRole.FINANCES));
        user.setId(UUID.randomUUID());
        user.setStatus(UserStatus.OK);
        Authentication authentication = mock(Authentication.class);

        when(authentication.getPrincipal()).thenReturn(user);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(tokenService.generateToken(user, AppScope.FINANCES)).thenReturn("jwt-token");
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoginResponseDTO result = service.login(
                new AuthRequestDTO("edu@test.com", "123", null, AppScope.FINANCES));

        assertEquals("jwt-token", result.token());
        assertEquals(List.of("FINANCES"), result.roles());
        assertEquals(List.of("FINANCES"), result.apps());
        verify(tokenService).generateToken(user, AppScope.FINANCES);
    }

    @Test
    void shouldRejectLoginWhenUserLacksRoleForRequestedApp() {
        User user = new User("Edu", "edu@test.com", "encoded", List.of(UserRole.STUDY));
        user.setId(UUID.randomUUID());
        user.setStatus(UserStatus.OK);
        Authentication authentication = mock(Authentication.class);

        when(authentication.getPrincipal()).thenReturn(user);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        assertThrows(CustomExceptions.AppAccessDeniedException.class,
                () -> service.login(new AuthRequestDTO("edu@test.com", "123", null, AppScope.FINANCES)));

        verify(tokenService, never()).generateToken(any(User.class), any(AppScope.class));
        verify(repository, never()).save(any(User.class));
    }

    @Test
    void shouldAllowAdminToLoginToAnyApp() {
        User user = new User("Admin", "admin@test.com", "encoded", List.of(UserRole.ADMIN));
        user.setId(UUID.randomUUID());
        user.setStatus(UserStatus.OK);
        Authentication authentication = mock(Authentication.class);

        when(authentication.getPrincipal()).thenReturn(user);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(tokenService.generateToken(user, AppScope.STUDY)).thenReturn("jwt-token");
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoginResponseDTO result = service.login(
                new AuthRequestDTO("admin@test.com", "123", null, AppScope.STUDY));

        assertEquals("jwt-token", result.token());
        verify(tokenService).generateToken(user, AppScope.STUDY);
    }

    @Test
    void shouldRejectLoginWhenUserIsNotActive() {
        User user = new User("Blocked", "blocked@test.com", "encoded", List.of(UserRole.FINANCES));
        user.setId(UUID.randomUUID());
        user.setStatus(UserStatus.BLOCKED);
        Authentication authentication = mock(Authentication.class);

        when(authentication.getPrincipal()).thenReturn(user);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        assertThrows(CustomExceptions.AccountNotActiveException.class,
                () -> service.login(new AuthRequestDTO("blocked@test.com", "123", null, AppScope.FINANCES)));

        verify(tokenService, never()).generateToken(any(User.class), any(AppScope.class));
        verify(repository, never()).save(any(User.class));
    }

    @Test
    void shouldAddRolesIncrementally() {
        authenticateAsAdmin();
        UUID id = UUID.randomUUID();
        User user = new User("Edu", "edu@test.com", "encoded", List.of(UserRole.BUDGETS));
        user.setId(id);

        when(repository.findById(id)).thenReturn(Optional.of(user));
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.addRoles(id, List.of(UserRole.STUDY));

        assertEquals(List.of(UserRole.BUDGETS, UserRole.STUDY), user.getRoles());
        assertEquals(1, user.getTokenVersion());
        verify(repository).save(user);
    }

    @Test
    void shouldRejectAdminRoleThroughAddRoles() {
        authenticateAsAdmin();
        UUID id = UUID.randomUUID();

        assertThrows(CustomExceptions.InvalidRoleException.class,
                () -> service.addRoles(id, List.of(UserRole.ADMIN)));

        verify(repository, never()).findById(any());
        verify(repository, never()).save(any());
    }

    @Test
    void shouldSendAppAccessEmailWhenRequestingAccess() {
        User user = new User("Edu", "edu@test.com", "encoded", List.of(UserRole.STUDY));
        user.setStatus(UserStatus.OK);

        when(repository.findAnyByEmail("edu@test.com")).thenReturn(Optional.of(user));
        when(tokenService.generateAppAccessToken(user, AppScope.FINANCES, "FINANCES")).thenReturn("token");

        service.requestAppAccess(new AppAccessRequestDTO("EDU@test.com", AppScope.FINANCES, null));

        verify(producer).publishAppAccessEmail(user, "FINANCES", "FINANCES", "token");
    }

    @Test
    void shouldStaySilentWhenRequestingAccessForUnknownEmail() {
        when(repository.findAnyByEmail("ghost@test.com")).thenReturn(Optional.empty());

        service.requestAppAccess(new AppAccessRequestDTO("ghost@test.com", AppScope.FINANCES, null));

        verifyNoInteractions(producer);
        verify(tokenService, never()).generateAppAccessToken(any(), any(), any());
    }

    @Test
    void shouldRejectAdminAppOnAccessRequest() {
        assertThrows(CustomExceptions.InvalidRoleException.class,
                () -> service.requestAppAccess(new AppAccessRequestDTO("edu@test.com", AppScope.ADMIN, null)));
    }

    @Test
    void shouldConfirmAppAccessAddingRoleAndActivatingAccount() {
        UUID id = UUID.randomUUID();
        User user = new User("Edu", "edu@test.com", "encoded", new java.util.ArrayList<>(List.of(UserRole.STUDY)));
        user.setId(id);
        user.setStatus(UserStatus.WAITING_CONFIRMATION);

        DecodedJWT jwt = mock(DecodedJWT.class);
        Claim appClaim = mock(Claim.class);
        Claim roleClaim = mock(Claim.class);

        when(tokenService.verifyActionToken("token", TokenService.PURPOSE_APP_ACCESS)).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn(id.toString());
        when(jwt.getClaim("app")).thenReturn(appClaim);
        when(appClaim.asString()).thenReturn("FINANCES");
        when(jwt.getClaim("role")).thenReturn(roleClaim);
        when(roleClaim.asString()).thenReturn("FINANCES");
        when(tokenService.tokenVersion(jwt)).thenReturn(0);
        when(repository.findById(id)).thenReturn(Optional.of(user));
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.confirmAppAccess(new ConfirmTokenRequestDTO("token"));

        assertEquals(UserStatus.OK, user.getStatus());
        assertTrue(user.getRoles().contains(UserRole.FINANCES));
        assertTrue(user.getRoles().contains(UserRole.STUDY));
        assertEquals(1, user.getTokenVersion());
    }

    @Test
    void shouldRejectAppAccessTokenWithWrongVersion() {
        UUID id = UUID.randomUUID();
        User user = new User("Edu", "edu@test.com", "encoded", new java.util.ArrayList<>(List.of(UserRole.STUDY)));
        user.setId(id);
        user.setStatus(UserStatus.OK);

        DecodedJWT jwt = mock(DecodedJWT.class);
        when(tokenService.verifyActionToken("token", TokenService.PURPOSE_APP_ACCESS)).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn(id.toString());
        when(tokenService.tokenVersion(jwt)).thenReturn(7);
        when(repository.findById(id)).thenReturn(Optional.of(user));

        assertThrows(CustomExceptions.InvalidTokenException.class,
                () -> service.confirmAppAccess(new ConfirmTokenRequestDTO("token")));
        verify(repository, never()).save(any());
    }

    @Test
    void shouldRejectAppAccessTokenContainingAdminRole() {
        UUID id = UUID.randomUUID();
        User user = new User("Edu", "edu@test.com", "encoded", new java.util.ArrayList<>(List.of(UserRole.STUDY)));
        user.setId(id);
        user.setStatus(UserStatus.OK);

        DecodedJWT jwt = mock(DecodedJWT.class);
        Claim appClaim = mock(Claim.class);
        Claim roleClaim = mock(Claim.class);

        when(tokenService.verifyActionToken("token", TokenService.PURPOSE_APP_ACCESS)).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn(id.toString());
        when(jwt.getClaim("app")).thenReturn(appClaim);
        when(appClaim.asString()).thenReturn("FINANCES");
        when(jwt.getClaim("role")).thenReturn(roleClaim);
        when(roleClaim.asString()).thenReturn("ADMIN");
        when(tokenService.tokenVersion(jwt)).thenReturn(0);
        when(repository.findById(id)).thenReturn(Optional.of(user));

        assertThrows(CustomExceptions.InvalidTokenException.class,
                () -> service.confirmAppAccess(new ConfirmTokenRequestDTO("token")));
        verify(repository, never()).save(any());
    }

    @Test
    void shouldSendPasswordResetEmailForActiveUser() {
        User user = new User("Edu", "edu@test.com", "encoded", List.of(UserRole.BUDGETS));
        user.setStatus(UserStatus.OK);

        when(repository.findAnyByEmail("edu@test.com")).thenReturn(Optional.of(user));
        when(tokenService.generatePasswordResetToken(user)).thenReturn("reset-token");

        service.forgotPassword(new ForgotPasswordRequestDTO("EDU@test.com"));

        verify(producer).publishPasswordResetEmail(user, "reset-token");
    }

    @Test
    void shouldStaySilentWhenEmailDoesNotExistOnPasswordResetRequest() {
        when(repository.findAnyByEmail("ghost@test.com")).thenReturn(Optional.empty());

        service.forgotPassword(new ForgotPasswordRequestDTO("ghost@test.com"));

        verifyNoInteractions(producer);
        verify(tokenService, never()).generatePasswordResetToken(any());
    }

    @Test
    void shouldStaySilentWhenUserIsBlockedOnPasswordResetRequest() {
        User user = new User("Edu", "edu@test.com", "encoded", List.of(UserRole.BUDGETS));
        user.setStatus(UserStatus.BLOCKED);

        when(repository.findAnyByEmail("edu@test.com")).thenReturn(Optional.of(user));

        service.forgotPassword(new ForgotPasswordRequestDTO("edu@test.com"));

        verify(tokenService, never()).generatePasswordResetToken(any());
        verifyNoInteractions(producer);
    }

    @Test
    void shouldResetPasswordAndInvalidateToken() {
        UUID id = UUID.randomUUID();
        User user = new User("Edu", "edu@test.com", "old-encoded", List.of(UserRole.BUDGETS));
        user.setId(id);
        user.setStatus(UserStatus.OK);

        DecodedJWT jwt = mock(DecodedJWT.class);
        when(tokenService.verifyActionToken("token", TokenService.PURPOSE_PASSWORD_RESET)).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn(id.toString());
        when(tokenService.tokenVersion(jwt)).thenReturn(0);
        when(repository.findById(id)).thenReturn(Optional.of(user));
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.resetPassword(new ResetPasswordRequestDTO("token", "novaSenha123"));

        assertNotNull(user.getPassword());
        assertFalse("old-encoded".equals(user.getPassword()));
        assertTrue(user.getPassword().startsWith("$2"));
        assertEquals(1, user.getTokenVersion());
        verify(repository).save(user);
    }

    @Test
    void shouldRejectAlreadyUsedPasswordResetToken() {
        UUID id = UUID.randomUUID();
        User user = new User("Edu", "edu@test.com", "old-encoded", List.of(UserRole.BUDGETS));
        user.setId(id);
        user.setStatus(UserStatus.OK);
        // Token emitido com ver=0, mas a senha já foi trocada (ver=1).
        user.setTokenVersion(1);

        DecodedJWT jwt = mock(DecodedJWT.class);
        when(tokenService.verifyActionToken("token", TokenService.PURPOSE_PASSWORD_RESET)).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn(id.toString());
        when(tokenService.tokenVersion(jwt)).thenReturn(0);
        when(repository.findById(id)).thenReturn(Optional.of(user));

        assertThrows(CustomExceptions.InvalidTokenException.class,
                () -> service.resetPassword(new ResetPasswordRequestDTO("token", "novaSenha123")));

        assertEquals("old-encoded", user.getPassword());
        verify(repository, never()).save(any());
    }

    @Test
    void shouldRejectInvalidPasswordResetToken() {
        when(tokenService.verifyActionToken("bad", TokenService.PURPOSE_PASSWORD_RESET)).thenReturn(null);

        assertThrows(CustomExceptions.InvalidTokenException.class,
                () -> service.resetPassword(new ResetPasswordRequestDTO("bad", "novaSenha123")));

        verify(repository, never()).save(any());
    }
}
