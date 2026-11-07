package com.edu.silva.study.domain.dto.requests

import java.util.*

data class ProjectVersionRequestDTO(
    val idProject: UUID,
    val title: String,
    val dueDate: Date
)
