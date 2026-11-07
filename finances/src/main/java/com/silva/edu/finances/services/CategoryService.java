package com.silva.edu.finances.services;

import com.silva.edu.finances.domain.dtos.request.AddCategoryRequestDTO;
import com.silva.edu.finances.domain.dtos.request.UpdateCategoryRequestDTO;
import com.silva.edu.finances.domain.dtos.response.CategoryResponseDTO;
import com.silva.edu.finances.domain.dtos.response.TransactionResponseDTO;
import com.silva.edu.finances.domain.entities.Category;
import lombok.NonNull;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface CategoryService {
    CategoryResponseDTO save(AddCategoryRequestDTO request);

    void delete(UUID id);

    Page<@NonNull CategoryResponseDTO> findAll(int page, int size);

    CategoryResponseDTO findById(UUID id);

    CategoryResponseDTO update(UUID id, UpdateCategoryRequestDTO request);

    Category ensureInvestmentCategory(UUID userId);
}
