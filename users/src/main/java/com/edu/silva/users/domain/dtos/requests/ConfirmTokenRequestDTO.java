package com.edu.silva.users.domain.dtos.requests;

import jakarta.validation.constraints.NotBlank;

public record ConfirmTokenRequestDTO(@NotBlank String token) {
}
