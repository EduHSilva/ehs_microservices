package com.edu.silva.study.domain.dto.responses

import java.util.UUID

data class ProjectStageResponseDTO(
    val id: UUID,
    val name: String,
    val done: Boolean
)
