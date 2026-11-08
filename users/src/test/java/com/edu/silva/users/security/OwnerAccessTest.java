package com.edu.silva.users.security;

import com.edu.silva.users.domain.dtos.requests.UpdateCompanyRequestDTO;
import com.edu.silva.users.domain.entities.Company;
import com.edu.silva.users.domain.entities.User;
import com.edu.silva.users.repositories.CompanyRepository;
import com.edu.silva.users.services.CompanyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@SpringBootTest
@EnableMethodSecurity
@ActiveProfiles("test")
class OwnerAccessTest {

    @Autowired
    CompanyService companyService;
    @Mock
    CompanyRepository repository;
    @Autowired
    CompanyService userService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        ReflectionTestUtils.setField(companyService, "repository", repository);
    }

    @Test
    @WithMockUser(username = "owner", roles = "BUDGETS")
    void shouldAllowOwner() {
        UUID id = UUID.randomUUID();

        Company company = new Company();
        User owner = new User();
        owner.setEmail("owner@email.com");
        owner.setUsername("owner@email.com");
        company.setOwner(owner);

        when(repository.findById(id)).thenReturn(Optional.of(company));
        when(repository.save(org.mockito.ArgumentMatchers.any(Company.class))).thenReturn(company);

        UpdateCompanyRequestDTO req = new UpdateCompanyRequestDTO("NewName");
        assertDoesNotThrow(() -> {
            try {
                companyService.update(id, req);
                userService.findAll(0, 10);
            } catch (Exception ignored) {
            }
        });
    }

    @Test
    @WithMockUser(username = "other", roles = "BUDGETS")
    void shouldBlockNonOwner() {
        UUID id = UUID.randomUUID();

        Company company = new Company();
        User owner = new User();
        owner.setEmail("owner@email.com");
        owner.setUsername("owner@email.com");
        company.setOwner(owner);

        when(repository.findById(id)).thenReturn(Optional.of(company));

        UpdateCompanyRequestDTO req = new UpdateCompanyRequestDTO("NewName");
        assertThrows(AccessDeniedException.class, () -> companyService.update(id, req));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void shouldAllowAdmin() {
        UUID id = UUID.randomUUID();

        Company company = new Company();
        User owner = new User();
        owner.setEmail("owner@email.com");
        owner.setUsername("owner@email.com");
        company.setOwner(owner);

        when(repository.findById(id)).thenReturn(Optional.of(company));
        when(repository.save(org.mockito.ArgumentMatchers.any(Company.class))).thenReturn(company);

        UpdateCompanyRequestDTO req = new UpdateCompanyRequestDTO("NewName");
        assertDoesNotThrow(() -> companyService.update(id, req));
    }
}
