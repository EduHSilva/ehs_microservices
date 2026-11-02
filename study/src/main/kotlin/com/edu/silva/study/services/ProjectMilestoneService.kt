package com.edu.silva.study.services

import com.edu.silva.study.domain.dto.requests.ProjectMilestoneRequestDTO
import com.edu.silva.study.domain.dto.requests.ProjectStageRequestDTO
import com.edu.silva.study.domain.dto.responses.ProjectMilestoneResponseDTO
import com.edu.silva.study.domain.dto.responses.ProjectStageResponseDTO
import java.util.*

interface ProjectMilestoneService {
    fun add(request: ProjectMilestoneRequestDTO?): ProjectMilestoneResponseDTO?

    fun update(id: UUID?, request: ProjectMilestoneRequestDTO?): ProjectMilestoneResponseDTO?
}
