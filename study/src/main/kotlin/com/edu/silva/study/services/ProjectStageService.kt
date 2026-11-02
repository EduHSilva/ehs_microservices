package com.edu.silva.study.services

import com.edu.silva.study.domain.dto.requests.ProjectStageRequestDTO
import com.edu.silva.study.domain.dto.responses.ProjectStageResponseDTO
import java.util.*

interface ProjectStageService {
    fun add(request: ProjectStageRequestDTO?): ProjectStageResponseDTO?

    fun update(id: UUID?, request: ProjectStageRequestDTO?): ProjectStageResponseDTO?
}
