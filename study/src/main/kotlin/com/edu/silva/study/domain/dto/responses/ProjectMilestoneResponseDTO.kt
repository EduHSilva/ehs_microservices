package com.edu.silva.study.domain.dto.responses

import java.util.Date
import java.util.UUID

data class ProjectMilestoneResponseDTO(
    val id: UUID,
    val title: String,
    val dueDate: Date
)
