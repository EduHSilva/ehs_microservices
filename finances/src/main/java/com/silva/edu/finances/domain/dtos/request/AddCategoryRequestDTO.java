package com.silva.edu.finances.domain.dtos.request;

import com.silva.edu.finances.domain.enums.CategoryClassification;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

public record AddCategoryRequestDTO(
        @NotBlank String title,
        @NotBlank String color,
        @NotNull @Min(0) Integer goal,
        @NotNull CategoryClassification classification
        ) {
}
