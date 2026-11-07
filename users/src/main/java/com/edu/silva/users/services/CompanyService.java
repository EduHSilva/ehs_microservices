package com.edu.silva.users.services;

import com.edu.silva.users.domain.dtos.requests.UpdateCompanyRequestDTO;
import com.edu.silva.users.domain.dtos.responses.CompanyResponseDTO;
import lombok.NonNull;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface CompanyService {
    Page<@NonNull CompanyResponseDTO> findAll(int page, int size);

    Page<@NonNull CompanyResponseDTO> search(String q, int page, int size, String sort, String direction);

    String exportCsv(String q);

    CompanyResponseDTO findById(UUID id);

    CompanyResponseDTO update(UUID id, UpdateCompanyRequestDTO request);

    void delete(UUID id);
}
