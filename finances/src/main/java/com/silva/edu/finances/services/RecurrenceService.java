package com.silva.edu.finances.services;

import com.silva.edu.finances.domain.dtos.request.AddCategoryRequestDTO;
import com.silva.edu.finances.domain.dtos.request.AddRecurrenceRequestDTO;
import com.silva.edu.finances.domain.dtos.request.UpdateCategoryRequestDTO;
import com.silva.edu.finances.domain.dtos.request.UpdateRecurrenceRequestDTO;
import com.silva.edu.finances.domain.dtos.response.CategoryResponseDTO;
import com.silva.edu.finances.domain.dtos.response.RecurrenceResponseDTO;
import lombok.NonNull;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.UUID;

public interface RecurrenceService {
    RecurrenceResponseDTO save(AddRecurrenceRequestDTO request);

    void delete(UUID id);

    Page<@NonNull RecurrenceResponseDTO> findAll(int page, int size, String accountId);

    RecurrenceResponseDTO findById(UUID id);

    RecurrenceResponseDTO update(UUID id, UpdateRecurrenceRequestDTO request);

    RecurrenceResponseDTO confirm(UUID id, BigDecimal value);
}
