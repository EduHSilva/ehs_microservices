package com.edu.silva.budgets.services;

import com.edu.silva.budgets.domain.dtos.requests.NewClientRequestDTO;
import com.edu.silva.budgets.domain.dtos.requests.UpdateClientRequestDTO;
import com.edu.silva.budgets.domain.dtos.responses.ClientResponseDTO;
import lombok.NonNull;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface ClientService {
    ClientResponseDTO save(NewClientRequestDTO request);
    void delete(UUID id);
    Page<@NonNull ClientResponseDTO> findAll(int page, int size, String name, String status);
    ClientResponseDTO findById(UUID id);
    ClientResponseDTO update(UUID id, UpdateClientRequestDTO request);
}
