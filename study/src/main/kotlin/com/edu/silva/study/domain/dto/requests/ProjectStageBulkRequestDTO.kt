package com.edu.silva.study.domain.dto.requests

import java.util.UUID

/**
 * Cria várias etapas de uma vez (ex.: módulos de um curso numerados pelo front).
 * Os títulos já chegam nomeados; o usuário pode renomeá-los depois.
 */
data class ProjectStageBulkRequestDTO(
    val idProject: UUID,
    val titles: List<String> = emptyList()
)
