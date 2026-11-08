package com.edu.silva.budgets.domain.dtos.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record NewClientRequestDTO(
        @NotBlank @Email String email,
        @NotBlank String name,
        @NotBlank String phone,
        String observations
) {
}
