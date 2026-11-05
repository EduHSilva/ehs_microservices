package com.silva.edu.finances.domain.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UpdateUserPreferenceRequestDTO(
        @NotNull Boolean showBusinessAccount,
        @NotBlank @Pattern(regexp = "BRL") String currency
) {}
