package com.edu.silva.study.domain.dto.requests

import java.util.*

data class ProjectStageRequestDTO(
    val idProject: UUID,
    val title: String,
    val done: Boolean
)
