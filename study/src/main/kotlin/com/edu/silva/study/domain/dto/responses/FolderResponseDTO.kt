package com.edu.silva.study.domain.dto.responses

import java.util.UUID

data class FolderResponseDTO (
    val id: UUID,
    val name: String?
)