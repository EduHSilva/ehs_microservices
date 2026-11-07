package com.edu.silva.users.services.impl;

import com.edu.silva.common.AuditLogger;
import com.edu.silva.common.enums.Status;
import com.edu.silva.users.domain.dtos.requests.UpdateCompanyRequestDTO;
import com.edu.silva.users.domain.dtos.responses.CompanyResponseDTO;
import com.edu.silva.users.domain.entities.Company;
import com.edu.silva.users.domain.entities.User;
import com.edu.silva.users.domain.enums.UserStatus;
import com.edu.silva.users.infra.exceptions.CustomExceptions;
import com.edu.silva.users.infra.security.annotattions.OnlyOwner;
import com.edu.silva.users.repositories.CompanyRepository;
import com.edu.silva.users.repositories.UserRepository;
import com.edu.silva.users.services.CompanyService;
import com.edu.silva.users.infra.security.annotattions.OnlyAdmin;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CompanyServiceImpl implements CompanyService {

    private static final List<String> SORT_FIELDS = List.of("name", "createdDate");

    private final CompanyRepository repository;
    private final UserRepository userRepository;

    public CompanyServiceImpl(CompanyRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    @Override
    @OnlyAdmin
    public Page<@NonNull CompanyResponseDTO> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return repository.findAll(pageable).map(CompanyResponseDTO::new);
    }

    @Override
    @OnlyAdmin
    @Transactional
    public Page<@NonNull CompanyResponseDTO> search(String q, int page, int size, String sort, String direction) {
        Pageable pageable = PageRequest.of(page, size, buildSort(sort, direction));
        return repository.findAll(specification(q), pageable).map(CompanyResponseDTO::new);
    }

    @Override
    @OnlyAdmin
    @Transactional
    public String exportCsv(String q) {
        List<Company> companies = repository.findAll(specification(q), Sort.by(Sort.Direction.DESC, "createdDate"));

        StringBuilder csv = new StringBuilder(CsvUtils.BOM);
        csv.append(CsvUtils.line("id", "name", "status", "users", "createdDate"));
        for (Company company : companies) {
            csv.append(CsvUtils.line(
                    company.getId(),
                    company.getName(),
                    company.getStatus(),
                    company.getUsers() == null ? 0 : company.getUsers().size(),
                    CsvUtils.date(company.getCreatedDate())
            ));
        }
        return csv.toString();
    }

    private Sort buildSort(String sort, String direction) {
        String property = SORT_FIELDS.contains(sort) ? sort : "createdDate";
        Sort.Direction order = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(order, property);
    }

    private Specification<Company> specification(String q) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (q != null && !q.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + q.trim().toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Override
    @OnlyAdmin
    public CompanyResponseDTO findById(UUID id) {
        return new CompanyResponseDTO(repository.findById(id)
                .orElseThrow(() -> new CustomExceptions.EntityNotFoundException(Company.class.getSimpleName(), id)));
    }

    @Override
    @OnlyOwner
    public CompanyResponseDTO update(UUID id, UpdateCompanyRequestDTO updateRequest) {
        return repository.findById(id)
                .map(company -> {
                    company.setName(updateRequest.name());
                    Company saved = repository.save(company);
                    AuditLogger.log(currentActor(), "COMPANY_UPDATE", Company.class.getSimpleName(), id,
                            Map.of("name", saved.getName()));
                    return new CompanyResponseDTO(saved);
                })
                .orElseThrow(() -> new CustomExceptions.EntityNotFoundException(Company.class.getSimpleName(), id));
    }

    @Override
    @OnlyAdmin
    @Transactional
    public void delete(UUID id) {
        Company company = repository.findById(id)
                .orElseThrow(() -> new CustomExceptions.EntityNotFoundException(Company.class.getSimpleName(), id));

        // Soft delete explícito: remover as entidades do contexto quebraria por
        // causa das referências (Company.owner e User.company).
        company.getUsers().forEach(user -> {
            user.setStatus(UserStatus.DELETED);
            userRepository.save(user);
        });

        company.setStatus(Status.DELETED);
        repository.save(company);

        AuditLogger.log(currentActor(), "COMPANY_DELETE", Company.class.getSimpleName(), id,
                Map.of("name", company.getName()));
    }

    private String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User principal)) {
            return "anonymous";
        }
        return String.valueOf(principal.getId());
    }
}
