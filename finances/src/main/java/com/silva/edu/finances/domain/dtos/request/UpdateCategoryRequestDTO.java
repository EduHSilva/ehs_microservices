package com.silva.edu.finances.domain.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateCategoryRequestDTO(
        @NotBlank String title,
        @NotNull @Min(0) Integer goal,
        @NotBlank String color) {

}
