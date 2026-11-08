package com.edu.silva.users.unit.services;

import com.edu.silva.users.domain.dtos.requests.RegisterRequestDTO;
import com.edu.silva.users.domain.dtos.requests.UpdateUserRequestDTO;
import com.edu.silva.users.domain.dtos.responses.UserResponseDTO;
import com.edu.silva.users.domain.dtos.responses.UserStatsResponseDTO;
import com.edu.silva.users.domain.entities.Company;
import com.edu.silva.users.domain.entities.User;
import com.edu.silva.users.domain.enums.UserRole;
import com.edu.silva.users.domain.enums.UserStatus;
import com.edu.silva.users.domain.producers.UserProducer;
import com.edu.silva.users.infra.exceptions.CustomExceptions;
import com.edu.silva.users.repositories.CompanyRepository;
import com.edu.silva.users.repositories.UserRepository;
import com.edu.silva.users.services.impl.UserServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
                "edu@test.com", "Eduardo", "123", UserRole.BUDGETS, null
        );

        when(repository.findByEmail("edu@test.com")).thenReturn(null);
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponseDTO saved = service.save(dto);

        assertEquals("Eduardo", saved.getUsername());
        assertEquals(UserStatus.WAITING_CONFIRMATION, saved.getStatus());

        verify(producer, times(1)).publishMessageEmail(any(User.class));
        verify(repository, times(1)).save(any(User.class));
    }

    @Test
    void shouldSaveUserSuccessfullyWithCompany() {
        UUID companyID = UUID.randomUUID();
        Company company = new Company();
        company.setId(companyID);
        RegisterRequestDTO dto = new RegisterRequestDTO(
                "edu@test.com", "Eduardo", "123", UserRole.BUDGETS, companyID
        );

        when(repository.findByEmail("edu@test.com")).thenReturn(null);
        when(companyRepository.findById(companyID)).thenReturn(Optional.of(company));
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponseDTO saved = service.save(dto);

        assertEquals("Eduardo", saved.getUsername());
        assertEquals(UserStatus.WAITING_CONFIRMATION, saved.getStatus());

        verify(producer, times(1)).publishMessageEmail(any(User.class));
        verify(repository, times(1)).save(any(User.class));
    }

    @Test
    void shouldThrowWhenCompanyNotFound() {
        RegisterRequestDTO dto = new RegisterRequestDTO(
                "edu@test.com", "Eduardo", "123", UserRole.BUDGETS, UUID.randomUUID()
        );

        when(companyRepository.findById(UUID.randomUUID())).thenReturn(Optional.empty());

        assertThrows(CustomExceptions.EntityNotFoundException.class, () -> service.save(dto));
    }

    @Test
    void shouldThrowWhenEmailAlreadyExists() {
        RegisterRequestDTO dto = new RegisterRequestDTO(
                "edu@test.com", "Eduardo", "123", UserRole.BUDGETS, null
        );

        when(repository.findByEmail("edu@test.com")).thenReturn(new User());

        assertThrows(CustomExceptions.EmailAlreadyExistsException.class, () -> service.save(dto));
    }

    @Test
    void shouldThrowWhenUserRegistersAsAdmin() {
        RegisterRequestDTO dto = new RegisterRequestDTO(
                "Eduardo", "edu@test.com", "123", UserRole.ADMIN, null
        );

        assertThrows(CustomExceptions.InvalidRoleException.class, () -> service.save(dto));
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
                "new@test.com",
                List.of(UserRole.ADMIN, UserRole.BUDGETS)
        );

        when(repository.findById(id)).thenReturn(Optional.of(user));
        when(repository.findByEmail("new@test.com")).thenReturn(null);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponseDTO updated = service.update(id, req);

        assertEquals("NewName", updated.getUsername());
        assertEquals("new@test.com", updated.getEmail());
        assertEquals(List.of(UserRole.ADMIN, UserRole.BUDGETS), updated.getRoles());
    }

    @Test
    void shouldAllowUserToAddNonAdminRoles() {
        authenticateAsUser();
        UUID id = UUID.randomUUID();
        User user = new User("Old", "old@test.com", "123", Collections.singletonList(UserRole.BUDGETS));

        UpdateUserRequestDTO req = new UpdateUserRequestDTO(
                "Old",
                null,
                List.of(UserRole.BUDGETS, UserRole.STUDY)
        );

        when(repository.findById(id)).thenReturn(Optional.of(user));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponseDTO updated = service.update(id, req);

        assertEquals(List.of(UserRole.BUDGETS, UserRole.STUDY), updated.getRoles());
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

        assertThrows(CustomExceptions.InvalidRoleException.class, () -> service.update(id, req));
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
        when(repository.findByEmail("used@test.com")).thenReturn(other);

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
}
