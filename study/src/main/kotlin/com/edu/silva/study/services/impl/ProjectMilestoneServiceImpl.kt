package com.edu.silva.study.services.impl

import com.edu.silva.study.domain.dto.requests.ProjectMilestoneRequestDTO
import com.edu.silva.study.domain.dto.responses.ProjectMilestoneResponseDTO
import com.edu.silva.study.domain.entities.ProjectMilestone
import com.edu.silva.study.domain.entities.Projects
import com.edu.silva.study.infra.exceptions.EntityNotFoundException
import com.edu.silva.study.infra.exceptions.InvalidRequestException
import com.edu.silva.study.repositories.ProjectMilestoneRepository
import com.edu.silva.study.repositories.ProjectRepository
import com.edu.silva.study.infra.config.UserContext
import com.edu.silva.study.services.ProjectMilestoneService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Date
import java.util.UUID

@Service
@Transactional
class ProjectMilestoneServiceImpl(
    private val repository: ProjectMilestoneRepository,
    private val projectRepository: ProjectRepository
) : ProjectMilestoneService {

    override fun add(request: ProjectMilestoneRequestDTO?): ProjectMilestoneResponseDTO {
        if (request == null) {
            throw InvalidRequestException("Project milestone request is required")
        }
        val milestone = ProjectMilestone(
            project = findProject(request.idProject),
            title = request.title,
            dueDate = request.dueDate,
            userId = UserContext.getUser()
        )
        return repository.save(milestone).toResponse()
    }

    override fun update(id: UUID?, request: ProjectMilestoneRequestDTO?): ProjectMilestoneResponseDTO {
        if (request == null) {
            throw InvalidRequestException("Project milestone request is required")
        }
        val milestone = findEntity(id)
        milestone.project = findProject(request.idProject)
        milestone.title = request.title
        milestone.dueDate = request.dueDate
        return repository.save(milestone).toResponse()
    }

    private fun findEntity(id: UUID?): ProjectMilestone {
        if (id == null) {
            throw EntityNotFoundException("Project milestone", id)
        }
        return repository.findByIdAndUserId(id, UserContext.getUser())
            .orElseThrow { EntityNotFoundException("Project milestone", id) }
    }

    private fun findProject(id: UUID): Projects {
        return projectRepository.findByIdAndUserId(id, UserContext.getUser())
            .orElseThrow { EntityNotFoundException("Project", id) }
    }

    private fun ProjectMilestone.toResponse(): ProjectMilestoneResponseDTO {
        return ProjectMilestoneResponseDTO(
            id = id!!,
            title = title,
            dueDate = dueDate ?: Date()
        )
    }
}
