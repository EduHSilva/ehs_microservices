package com.edu.silva.users.controllers;

import com.edu.silva.users.domain.dtos.requests.UpdateUserRequestDTO;
import com.edu.silva.users.domain.dtos.responses.UserResponseDTO;
import com.edu.silva.users.domain.entities.User;
import com.edu.silva.users.domain.enums.UserRole;
import com.edu.silva.users.services.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class UserControllerAccessTest {

    @Autowired
    private UserController controller;

    @MockitoBean
    private UserService service;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(UserRole role, UUID id) {
        User user = new User("test", "test@test.com", "123", List.of(role));
        user.setId(id);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );
    }

    @Test
    void shouldBlockNonAdminFromListingUsers() {
        authenticate(UserRole.CRM, UUID.randomUUID());

        assertThrows(AccessDeniedException.class, () -> controller.all(0, 10, null, null, null, "createdDate", "desc"));
    }

    @Test
    void shouldAllowAdminToListUsers() {
        authenticate(UserRole.ADMIN, UUID.randomUUID());
        when(service.search(null, null, null, 0, 10, "createdDate", "desc")).thenReturn(new PageImpl<>(List.of(new UserResponseDTO())));

        assertDoesNotThrow(() -> controller.all(0, 10, null, null, null, "createdDate", "desc"));
    }

    @Test
    void shouldBlockNonAdminFromDeletingUsers() {
        authenticate(UserRole.CRM, UUID.randomUUID());

        assertThrows(AccessDeniedException.class, () -> controller.delete(UUID.randomUUID()));
    }

    @Test
    void shouldAllowAdminToDeleteUsers() {
        authenticate(UserRole.ADMIN, UUID.randomUUID());
        UUID targetId = UUID.randomUUID();

        assertDoesNotThrow(() -> controller.delete(targetId));
        verify(service).delete(targetId);
    }

    @Test
    void shouldAllowUserToReadOwnData() {
        UUID ownId = UUID.randomUUID();
        authenticate(UserRole.CRM, ownId);
        when(service.findById(ownId)).thenReturn(new UserResponseDTO());

        assertDoesNotThrow(() -> controller.one(ownId));
    }

    @Test
    void shouldBlockUserFromReadingAnotherUser() {
        authenticate(UserRole.CRM, UUID.randomUUID());

        assertThrows(AccessDeniedException.class, () -> controller.one(UUID.randomUUID()));
    }

    @Test
    void shouldAllowUserToUpdateOwnData() {
        UUID ownId = UUID.randomUUID();
        authenticate(UserRole.CRM, ownId);
        when(service.update(eq(ownId), any())).thenReturn(new UserResponseDTO());

        assertDoesNotThrow(() -> controller.update(new UpdateUserRequestDTO("new name"), ownId));
    }

    @Test
    void shouldBlockUserFromUpdatingAnotherUser() {
        authenticate(UserRole.CRM, UUID.randomUUID());

        assertThrows(
                AccessDeniedException.class,
                () -> controller.update(new UpdateUserRequestDTO("new name"), UUID.randomUUID())
        );
    }

    @Test
    void shouldAllowAdminToUpdateAnotherUser() {
        authenticate(UserRole.ADMIN, UUID.randomUUID());
        UUID targetId = UUID.randomUUID();
        when(service.update(eq(targetId), any())).thenReturn(new UserResponseDTO());

        assertDoesNotThrow(() -> controller.update(new UpdateUserRequestDTO("new name"), targetId));
    }
}
