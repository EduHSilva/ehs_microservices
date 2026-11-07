package com.edu.silva.study.services.impl

import com.edu.silva.study.domain.dto.requests.ProjectVersionRequestDTO
import com.edu.silva.study.domain.dto.responses.ProjectVersionResponseDTO
import com.edu.silva.study.domain.entities.ProjectVersion
import com.edu.silva.study.domain.entities.Projects
import com.edu.silva.study.infra.exceptions.EntityNotFoundException
import com.edu.silva.study.infra.exceptions.InvalidRequestException
import com.edu.silva.study.repositories.ProjectVersionRepository
import com.edu.silva.study.repositories.ProjectRepository
import com.edu.silva.study.infra.config.UserContext
import com.edu.silva.study.services.ProjectVersionService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Date
import java.util.UUID

@Service
@Transactional
class ProjectVersionServiceImpl(
    private val repository: ProjectVersionRepository,
    private val projectRepository: ProjectRepository
) : ProjectVersionService {

    override fun add(request: ProjectVersionRequestDTO?): ProjectVersionResponseDTO {
        if (request == null) {
            throw InvalidRequestException("Project version request is required")
        }
        val version = ProjectVersion(
            project = findProject(request.idProject),
            title = request.title,
            dueDate = request.dueDate,
            userId = UserContext.getUser()
        )
        return repository.save(version).toResponse()
    }

    override fun update(id: UUID?, request: ProjectVersionRequestDTO?): ProjectVersionResponseDTO {
        if (request == null) {
            throw InvalidRequestException("Project version request is required")
        }
        val version = findEntity(id)
        version.project = findProject(request.idProject)
        version.title = request.title
        version.dueDate = request.dueDate
        return repository.save(version).toResponse()
    }

    private fun findEntity(id: UUID?): ProjectVersion {
        if (id == null) {
            throw EntityNotFoundException("Project version", id)
        }
        return repository.findByIdAndUserId(id, UserContext.getUser())
            .orElseThrow { EntityNotFoundException("Project version", id) }
    }

    private fun findProject(id: UUID): Projects {
        return projectRepository.findByIdAndUserId(id, UserContext.getUser())
            .orElseThrow { EntityNotFoundException("Project", id) }
    }

    private fun ProjectVersion.toResponse(): ProjectVersionResponseDTO {
        return ProjectVersionResponseDTO(
            id = id!!,
            title = title,
            dueDate = dueDate ?: Date()
        )
    }
}
