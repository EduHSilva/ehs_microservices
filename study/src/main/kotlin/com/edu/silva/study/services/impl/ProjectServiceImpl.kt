package com.edu.silva.study.services.impl

import com.edu.silva.study.domain.dto.requests.ProjectRequestDTO
import com.edu.silva.study.domain.dto.responses.ProjectResponseDTO
import com.edu.silva.study.services.ProjectService
import org.springframework.data.domain.Page
import org.springframework.stereotype.Service
import java.util.*

@Service
class ProjectServiceImpl : ProjectService{
    override fun findAll(
        page: Int,
        size: Int
    ): Page<ProjectResponseDTO>? {
        TODO("Not yet implemented")
    }

    override fun findById(id: UUID?): ProjectResponseDTO? {
        TODO("Not yet implemented")
    }

    override fun add(request: ProjectRequestDTO?): ProjectResponseDTO? {
        TODO("Not yet implemented")
    }

    override fun update(
        id: UUID?,
        request: ProjectRequestDTO?
    ): ProjectResponseDTO? {
        TODO("Not yet implemented")
    }

}