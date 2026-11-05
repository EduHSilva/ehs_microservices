package com.silva.edu.finances.services;

import com.silva.edu.finances.domain.dtos.request.AddTransactionRequestDTO;
import com.silva.edu.finances.domain.dtos.request.UpdateTransactionRequestDTO;
import com.silva.edu.finances.domain.dtos.response.TransactionResponseDTO;
import lombok.NonNull;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface TransactionService {
    TransactionResponseDTO save(AddTransactionRequestDTO request);

    void delete(UUID id);

    Page<@NonNull TransactionResponseDTO> findAll(int page, int size, String accountId);

    TransactionResponseDTO findById(UUID id);

    TransactionResponseDTO update(UUID id, UpdateTransactionRequestDTO request);
}
