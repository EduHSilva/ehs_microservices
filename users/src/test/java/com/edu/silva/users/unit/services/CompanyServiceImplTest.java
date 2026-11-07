package com.edu.silva.users.unit.services;

import com.edu.silva.common.enums.Status;
import com.edu.silva.users.domain.dtos.requests.UpdateCompanyRequestDTO;
import com.edu.silva.users.domain.dtos.responses.CompanyResponseDTO;
import com.edu.silva.users.domain.entities.Company;
import com.edu.silva.users.domain.entities.User;
import com.edu.silva.users.domain.enums.UserStatus;
import com.edu.silva.users.infra.exceptions.CustomExceptions;
import com.edu.silva.users.repositories.CompanyRepository;
import com.edu.silva.users.repositories.UserRepository;
import com.edu.silva.users.services.impl.CompanyServiceImpl;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompanyServiceImplTest {

    @Mock
    private CompanyRepository repository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CompanyServiceImpl service;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldReturnPagedCompanies() {
        int page = 0;
        int size = 2;

        Pageable pageable = PageRequest.of(page, size);

        Company company = new Company();
        Company company1 = new Company();

        List<Company> companies = List.of(company, company1);

        Page<Company> pageComp = new PageImpl<>(companies, pageable, companies.size());

        when(repository.findAll(pageable)).thenReturn(pageComp);

        Page<CompanyResponseDTO> result = service.findAll(page, size);

        assertEquals(2, result.getContent().size());
        verify(repository).findAll(pageable);
    }

    @Test
    void shouldReturnCompany() {
        int page = 0;
        int size = 2;

        Pageable pageable = PageRequest.of(page, size);

        Company company = new Company();
        Company company1 = new Company();

        List<Company> companies = List.of(company, company1);

        Page<Company> pageComp = new PageImpl<>(companies, pageable, companies.size());

        when(repository.findAll(pageable)).thenReturn(pageComp);

        Page<CompanyResponseDTO> result = service.findAll(page, size);

        assertEquals(2, result.getContent().size());
        verify(repository).findAll(pageable);
    }

    @Test
    void shouldReturnCompanyWhenFound() {
        UUID id = UUID.randomUUID();
        Company company = new Company();
        when(repository.findById(id)).thenReturn(Optional.of(company));

        CompanyResponseDTO result = service.findById(id);
        assertNotNull(result);
    }

    @Test
    void shouldThrowWhenCompanyNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(CustomExceptions.EntityNotFoundException.class, () -> service.findById(id));
    }

    @Test
    void shouldSearchCompaniesByName() {
        Company company = new Company();
        company.setName("Acme");

        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(company)));

        Page<CompanyResponseDTO> result = service.search("acme", 0, 10, "name", "asc");

        assertEquals(1, result.getContent().size());
        assertEquals("Acme", result.getContent().get(0).getName());
    }

    @Test
    void shouldExportCompaniesCsv() {
        Company company = new Company();
        company.setName("Acme");
        company.setStatus(Status.ACTIVE);

        when(repository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(company));

        String csv = service.exportCsv(null);

        assertTrue(csv.contains("name"));
        assertTrue(csv.contains("Acme"));
    }

    @Test
    void shouldUpdateCompany() {
        UUID id = UUID.randomUUID();

        Company company = new Company();
        company.setName("Old");
        UpdateCompanyRequestDTO req = new UpdateCompanyRequestDTO("NewName");

        when(repository.findById(id)).thenReturn(Optional.of(company));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CompanyResponseDTO updated = service.update(id, req);

        assertEquals("NewName", updated.getName());
    }

    @Test
    void shouldThrowWhenUpdatingNonExistingCompany() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        UpdateCompanyRequestDTO req = new UpdateCompanyRequestDTO("Test");

        assertThrows(CustomExceptions.EntityNotFoundException.class, () -> service.update(id, req));
    }

    @Test
    void shouldSoftDeleteCompanyAndItsUsers() {
        UUID id = UUID.randomUUID();
        Company company = new Company();
        company.setName("Test");
        User user = new User();
        user.setStatus(UserStatus.OK);
        company.setUsers(List.of(user));

        when(repository.findById(id)).thenReturn(Optional.of(company));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.delete(id);

        assertEquals(UserStatus.DELETED, user.getStatus());
        assertEquals(Status.DELETED, company.getStatus());
        verify(userRepository).save(user);
        verify(repository).save(company);
        verify(repository, never()).delete(any(Company.class));
    }

    @Test
    void shouldThrowWhenDeletingNonExistingCompany() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(CustomExceptions.EntityNotFoundException.class, () -> service.delete(id));
    }
}
