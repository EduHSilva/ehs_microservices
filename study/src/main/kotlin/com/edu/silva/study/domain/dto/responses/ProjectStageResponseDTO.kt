package com.edu.silva.study.domain.dto.responses

import java.util.UUID

data class ProjectStageResponseDTO(
    val id: UUID,
    val name: String,
    val done: Boolean,
    /** Progresso do projeto depois da alteração; nulo quando a etapa vem dentro do projeto. */
    val projectProgress: Int? = null
)
