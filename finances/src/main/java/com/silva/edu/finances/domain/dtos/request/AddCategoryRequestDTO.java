package com.silva.edu.finances.domain.dtos.request;

import com.silva.edu.finances.domain.enums.CategoryClassification;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddCategoryRequestDTO(
        @NotBlank String title,
        @NotBlank String color,
        @NotNull @Min(0) @Max(100) Integer goal,
        CategoryClassification classification,
        Boolean income
        ) {
}
