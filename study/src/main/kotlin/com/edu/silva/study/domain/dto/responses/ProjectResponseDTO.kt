package com.edu.silva.study.domain.dto.responses

import com.edu.silva.study.domain.enums.ProjectStatus
import java.util.UUID

data class ProjectResponseDTO(
    val id: UUID,
    val title: String,
    val versions: ArrayList<ProjectVersionResponseDTO>,
    val stages: ArrayList<ProjectStageResponseDTO>,
    val status: ProjectStatus,
    val progress: Int,
    val advisor: String
)
