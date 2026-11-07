package com.edu.silva.users.unit.controllers;

import com.edu.silva.users.controllers.CompanyController;
import com.edu.silva.users.domain.dtos.requests.UpdateCompanyRequestDTO;
import com.edu.silva.users.domain.dtos.responses.CompanyResponseDTO;
import com.edu.silva.users.services.CompanyService;
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
class CompanyControllerTest {

    private RestTestClient client;

    @Mock
    private CompanyService service;

    @BeforeEach
    void setUp() {
        client = RestTestClient.bindToController(new CompanyController(service))
                .defaultHeader("ContentType", "application/json")
                .build();
    }

    @Test
    void shouldReturnCompanies() {
        List<CompanyResponseDTO> companies = List.of(new CompanyResponseDTO());
        Page<CompanyResponseDTO> page =
                new PageImpl<>(companies, PageRequest.of(0, 10), companies.size());

        when(service.search(null, 0, 10, "createdDate", "desc")).thenReturn(page);

        client.get()
                .uri("/companies")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Find all companies successfully")
                .jsonPath("$.data.items").isArray();
    }

    @Test
    void shouldExportCompaniesCsv() {
        when(service.exportCsv(null)).thenReturn("\uFEFF\"id\"\n");

        client.get()
                .uri("/companies/export")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueMatches("Content-Disposition", "attachment;.*empresas\\.csv.*")
                .expectHeader().valueMatches("Content-Type", "text/csv.*");
    }

    @Test
    void shouldFindCompany() {
        UUID id = UUID.randomUUID();

        when(service.findById(id)).thenReturn(new CompanyResponseDTO());

        client.get()
                .uri("/companies/" + id)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Company find successfully")
                .jsonPath("$.data").isNotEmpty();
    }

    @Test
    void shouldUpdateCompany() {
        UUID id = UUID.randomUUID();
        UpdateCompanyRequestDTO requestDTO = new UpdateCompanyRequestDTO(RandomStringUtils.random(10));
        when(service.update(id, requestDTO)).thenReturn(new CompanyResponseDTO());

        client.put()
                .uri("/companies/" + id)
                .body(requestDTO)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Company successfully updated")
                .jsonPath("$.data").isNotEmpty();
    }

    @Test
    void shouldDeleteCompany() {
        UUID id = UUID.randomUUID();

        client.delete()
                .uri("/companies/" + id)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Company successfully deleted");
    }
}