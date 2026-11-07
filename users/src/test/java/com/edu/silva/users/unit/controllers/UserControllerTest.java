package com.edu.silva.users.unit.controllers;

import com.edu.silva.users.controllers.UserController;
import com.edu.silva.users.domain.dtos.requests.RegisterRequestDTO;
import com.edu.silva.users.domain.dtos.requests.UpdateUserRequestDTO;
import com.edu.silva.users.domain.dtos.responses.UserResponseDTO;
import com.edu.silva.users.domain.dtos.responses.UserStatsResponseDTO;
import com.edu.silva.users.domain.enums.UserRole;
import com.edu.silva.users.services.UserService;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private RestTestClient client;

    @Mock
    private UserService service;

    @BeforeEach
    void setUp() {
        client = RestTestClient.bindToController(new UserController(service))
                .defaultHeader("ContentType", "application/json")
                .build();
    }

    @Test
    void shouldReturnUsers() {
        List<UserResponseDTO> users = List.of(new UserResponseDTO());
        Page<UserResponseDTO> page =
                new PageImpl<>(users, PageRequest.of(0, 10), users.size());

        when(service.search(null, null, null, 0, 10, "createdDate", "desc")).thenReturn(page);

        client.get()
                .uri("/")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Find all users successfully")
                .jsonPath("$.data.items").isArray();
    }

    @Test
    void shouldExportUsersCsv() {
        when(service.exportCsv(null, null, null)).thenReturn("\uFEFF\"id\"\n");

        client.get()
                .uri("/export")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueMatches("Content-Disposition", "attachment;.*usuarios\\.csv.*")
                .expectHeader().valueMatches("Content-Type", "text/csv.*");
    }

    @Test
    void shouldReturnStats() {
        when(service.stats(null, null, "day"))
                .thenReturn(new UserStatsResponseDTO(1, 1, 0, 0, 0, List.of()));

        client.get()
                .uri("/stats")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("User statistics successfully retrieved")
                .jsonPath("$.data.totalUsers").isEqualTo(1);
    }

    @Test
    void shouldFindUser() {
        UUID userId = UUID.randomUUID();

        when(service.findById(userId)).thenReturn(new UserResponseDTO());

        client.get()
                .uri("/" + userId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("User find successfully")
                .jsonPath("$.data").isNotEmpty();
    }

    @Test
    void shouldUpdateUser() {
        UUID userId = UUID.randomUUID();
        UpdateUserRequestDTO requestDTO = new UpdateUserRequestDTO(RandomStringUtils.random(10));
        when(service.update(userId, requestDTO)).thenReturn(new UserResponseDTO());

        client.put()
                .uri("/" + userId)
                .body(requestDTO)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("User successfully updated")
                .jsonPath("$.data").isNotEmpty();
    }

    @Test
    void shouldDeletedUser() {
        UUID userId = UUID.randomUUID();

        client.delete()
                .uri("/" + userId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("User successfully deleted")
                .jsonPath("$.data").isEmpty();
    }

    @Test
    void shouldConfirmEmail() {
        UUID userId = UUID.randomUUID();

        client.get()
                .uri("/confirm/" + userId)
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/confirm.html");    }


}