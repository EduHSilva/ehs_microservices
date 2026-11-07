package com.edu.silva.study.services

import com.edu.silva.study.domain.dto.requests.ProjectVersionRequestDTO
import com.edu.silva.study.domain.dto.responses.ProjectVersionResponseDTO
import java.util.*

interface ProjectVersionService {
    fun add(request: ProjectVersionRequestDTO?): ProjectVersionResponseDTO?

    fun update(id: UUID?, request: ProjectVersionRequestDTO?): ProjectVersionResponseDTO?
}
