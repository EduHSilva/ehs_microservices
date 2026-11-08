package com.edu.silva.budgets.domain.dtos.requests;

import com.edu.silva.budgets.domain.enums.ClientStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateClientRequestDTO(
        @NotBlank String name,
        @Email String email,
        String phone,
        String observations,
        ClientStatus status
) {
}
