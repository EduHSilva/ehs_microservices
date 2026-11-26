package com.edu.silva.study.services

import com.edu.silva.study.domain.dto.requests.ProjectStageBulkRequestDTO
import com.edu.silva.study.domain.dto.requests.ProjectStageRequestDTO
import com.edu.silva.study.domain.dto.responses.ProjectStageBulkResponseDTO
import com.edu.silva.study.domain.dto.responses.ProjectStageResponseDTO
import java.util.*

interface ProjectStageService {
    fun add(request: ProjectStageRequestDTO?): ProjectStageResponseDTO?

    fun addMany(request: ProjectStageBulkRequestDTO?): ProjectStageBulkResponseDTO?

    fun update(id: UUID?, request: ProjectStageRequestDTO?): ProjectStageResponseDTO?
}
