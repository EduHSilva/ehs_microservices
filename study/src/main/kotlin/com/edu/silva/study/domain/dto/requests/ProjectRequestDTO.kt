package com.edu.silva.study.domain.dto.requests

import com.edu.silva.study.domain.enums.ProjectStatus
import java.util.UUID

data class ProjectRequestDTO(
    val id: UUID? = null,
    val title: String,
    val status: ProjectStatus? = null,
    val advisor: String? = null
)
