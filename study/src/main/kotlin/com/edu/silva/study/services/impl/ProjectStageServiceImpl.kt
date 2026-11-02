package com.edu.silva.study.services.impl

import com.edu.silva.study.domain.dto.requests.ProjectStageRequestDTO
import com.edu.silva.study.domain.dto.responses.ProjectStageResponseDTO
import com.edu.silva.study.domain.entities.ProjectStage
import com.edu.silva.study.domain.entities.Projects
import com.edu.silva.study.infra.exceptions.EntityNotFoundException
import com.edu.silva.study.infra.exceptions.InvalidRequestException
import com.edu.silva.study.repositories.ProjectRepository
import com.edu.silva.study.infra.config.UserContext
import com.edu.silva.study.repositories.ProjectStageRepository
import com.edu.silva.study.services.ProjectStageService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class ProjectStageServiceImpl(
    private val repository: ProjectStageRepository,
    private val projectRepository: ProjectRepository
) : ProjectStageService {

    override fun add(request: ProjectStageRequestDTO?): ProjectStageResponseDTO {
        if (request == null) {
            throw InvalidRequestException("Project stage request is required")
        }
        val stage = ProjectStage(
            project = findProject(request.idProject),
            name = request.title,
            done = request.done,
            userId = UserContext.getUser()
        )
        return repository.save(stage).toResponse()
    }

    override fun update(id: UUID?, request: ProjectStageRequestDTO?): ProjectStageResponseDTO {
        if (request == null) {
            throw InvalidRequestException("Project stage request is required")
        }
        val stage = findEntity(id)
        stage.project = findProject(request.idProject)
        stage.name = request.title
        stage.done = request.done
        return repository.save(stage).toResponse()
    }

    private fun findEntity(id: UUID?): ProjectStage {
        if (id == null) {
            throw EntityNotFoundException("Project stage", id)
        }
        return repository.findByIdAndUserId(id, UserContext.getUser())
            .orElseThrow { EntityNotFoundException("Project stage", id) }
    }

    private fun findProject(id: UUID): Projects {
        return projectRepository.findByIdAndUserId(id, UserContext.getUser())
            .orElseThrow { EntityNotFoundException("Project", id) }
    }

    private fun ProjectStage.toResponse(): ProjectStageResponseDTO {
        return ProjectStageResponseDTO(
            id = id!!,
            name = name,
            done = done
        )
    }
}
