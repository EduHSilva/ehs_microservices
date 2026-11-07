package com.edu.silva.study.services.impl

import com.edu.silva.study.domain.dto.requests.ProjectRequestDTO
import com.edu.silva.study.domain.dto.responses.ProjectVersionResponseDTO
import com.edu.silva.study.domain.dto.responses.ProjectResponseDTO
import com.edu.silva.study.domain.dto.responses.ProjectStageResponseDTO
import com.edu.silva.study.domain.entities.ProjectVersion
import com.edu.silva.study.domain.entities.ProjectStage
import com.edu.silva.study.domain.entities.Projects
import com.edu.silva.study.infra.exceptions.EntityNotFoundException
import com.edu.silva.study.infra.exceptions.InvalidRequestException
import com.edu.silva.study.repositories.ProjectRepository
import com.edu.silva.study.infra.config.UserContext
import com.edu.silva.study.services.ProjectService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Date
import java.util.UUID

@Service
@Transactional
class ProjectServiceImpl(
    private val repository: ProjectRepository
) : ProjectService {

    override fun findAll(page: Int, size: Int): Page<ProjectResponseDTO> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"))
        return repository.findAll(pageable).map { it.toResponse() }
    }

    override fun findById(id: UUID?): ProjectResponseDTO {
        return findEntity(id).toResponse()
    }

    override fun add(request: ProjectRequestDTO?): ProjectResponseDTO {
        if (request == null) {
            throw InvalidRequestException("Project request is required")
        }
        val project = Projects(
            title = request.title,
            status = request.status,
            advisor = request.advisor,
            userId = UserContext.getUser()
        )
        return repository.save(project).toResponse()
    }

    override fun update(id: UUID?, request: ProjectRequestDTO?): ProjectResponseDTO {
        if (request == null) {
            throw InvalidRequestException("Project request is required")
        }
        val project = findEntity(id)
        project.title = request.title
        project.status = request.status
        request.advisor?.let { project.advisor = it }
        return repository.save(project).toResponse()
    }

    private fun findEntity(id: UUID?): Projects {
        if (id == null) {
            throw EntityNotFoundException("Project", id)
        }
        return repository.findByIdAndUserId(id, UserContext.getUser())
            .orElseThrow { EntityNotFoundException("Project", id) }
    }

    private fun Projects.toResponse(): ProjectResponseDTO {
        val stages = stages.sortedBy { it.createdDate }
        val progress = if (stages.isEmpty()) 0 else stages.count { it.done } * 100 / stages.size
        return ProjectResponseDTO(
            id = id!!,
            title = title,
            versions = versions.map { it.toResponse() }.toCollection(ArrayList()),
            stages = stages.map { it.toResponse() }.toCollection(ArrayList()),
            status = status,
            progress = progress,
            advisor = advisor ?: ""
        )
    }

    private fun ProjectVersion.toResponse(): ProjectVersionResponseDTO {
        return ProjectVersionResponseDTO(
            id = id!!,
            title = title,
            dueDate = dueDate ?: Date()
        )
    }

    private fun ProjectStage.toResponse(): ProjectStageResponseDTO {
        return ProjectStageResponseDTO(
            id = id!!,
            name = name,
            done = done
        )
    }
}
