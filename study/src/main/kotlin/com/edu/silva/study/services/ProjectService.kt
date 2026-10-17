package com.edu.silva.study.services

import com.edu.silva.study.domain.dto.requests.ProjectRequestDTO
import com.edu.silva.study.domain.dto.responses.ProjectResponseDTO
import org.springframework.data.domain.Page
import java.util.*

interface ProjectService {
    fun findAll(page: Int, size: Int): Page<ProjectResponseDTO>?

    fun findById(id: UUID?): ProjectResponseDTO?

    fun add(request: ProjectRequestDTO?): ProjectResponseDTO?

    fun update(id: UUID?, request: ProjectRequestDTO?): ProjectResponseDTO?
}
