package com.edu.silva.study.domain.dto.requests

import java.util.*

data class ProjectMilestoneRequestDTO(
    val idProject: UUID,
    val title: String,
    val dueDate: Date
)
