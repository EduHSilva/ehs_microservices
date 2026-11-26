package com.edu.silva.study.domain.dto.responses

data class ProjectStageBulkResponseDTO(
    val stages: ArrayList<ProjectStageResponseDTO>,
    val projectProgress: Int
)
