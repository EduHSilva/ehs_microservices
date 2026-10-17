package com.edu.silva.study.domain.dto.requests

import jakarta.validation.constraints.NotBlank

data class FolderRequestDTO (
    @NotBlank val name: String
)