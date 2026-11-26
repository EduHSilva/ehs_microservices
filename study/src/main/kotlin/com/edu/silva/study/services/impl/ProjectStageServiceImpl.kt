package com.edu.silva.study.services.impl

import com.edu.silva.study.domain.dto.requests.ProjectStageBulkRequestDTO
import com.edu.silva.study.domain.dto.requests.ProjectStageRequestDTO
import com.edu.silva.study.domain.dto.responses.ProjectStageBulkResponseDTO
import com.edu.silva.study.domain.dto.responses.ProjectStageResponseDTO
import com.edu.silva.study.domain.entities.ProjectStage
import com.edu.silva.study.domain.entities.Projects
import com.edu.silva.study.infra.exceptions.EntityNotFoundException
import com.edu.silva.study.infra.exceptions.InvalidRequestException
import com.edu.silva.study.repositories.ProjectRepository
import com.edu.silva.study.infra.config.UserContext
import com.edu.silva.study.repositories.ProjectStageRepository
import com.edu.silva.study.services.ProjectProgressService
import com.edu.silva.study.services.ProjectStageService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class ProjectStageServiceImpl(
    private val repository: ProjectStageRepository,
    private val projectRepository: ProjectRepository,
    private val progressService: ProjectProgressService
) : ProjectStageService {

    override fun add(request: ProjectStageRequestDTO?): ProjectStageResponseDTO {
        if (request == null) {
            throw InvalidRequestException("Project stage request is required")
        }
        val stage = ProjectStage(
            project = findProject(request.idProject),
            name = requireTitle(request.title),
            done = request.done,
            userId = UserContext.getUser()
        )
        return repository.save(stage).toResponse(progressOf(stage))
    }

    override fun addMany(request: ProjectStageBulkRequestDTO?): ProjectStageBulkResponseDTO? {
        if (request == null) {
            throw InvalidRequestException("Project stage request is required")
        }
        if (request.titles.isEmpty()) {
            throw InvalidRequestException("At least one stage title is required")
        }
        if (request.titles.size > MAX_BATCH) {
            throw InvalidRequestException("At most $MAX_BATCH stages can be created at once")
        }
        val project = findProject(request.idProject)
        val userId = UserContext.getUser()
        val saved = repository.saveAll(
            request.titles.map { title ->
                ProjectStage(
                    project = project,
                    name = requireTitle(title),
                    done = false,
                    userId = userId
                )
            }
        )
        val projectId = project.id!!
        val progress = progressService.progressOf(projectId, repository.findAllByProject_IdAndUserId(projectId, userId))
        return ProjectStageBulkResponseDTO(
            stages = saved.map { it.toResponse(progress) }.toCollection(ArrayList()),
            projectProgress = progress
        )
    }

    override fun update(id: UUID?, request: ProjectStageRequestDTO?): ProjectStageResponseDTO {
        if (request == null) {
            throw InvalidRequestException("Project stage request is required")
        }
        val stage = findEntity(id)
        stage.project = findProject(request.idProject)
        stage.name = requireTitle(request.title)
        stage.done = request.done
        return repository.save(stage).toResponse(progressOf(stage))
    }

    private fun requireTitle(value: String): String {
        val title = value.trim()
        if (title.isEmpty()) {
            throw InvalidRequestException("Project stage title is required")
        }
        return title
    }

    private fun progressOf(stage: ProjectStage): Int {
        val projectId = stage.project!!.id!!
        val stages = repository.findAllByProject_IdAndUserId(projectId, UserContext.getUser())
        return progressService.progressOf(projectId, stages)
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

    private fun ProjectStage.toResponse(projectProgress: Int): ProjectStageResponseDTO {
        return ProjectStageResponseDTO(
            id = id!!,
            name = name,
            done = done,
            projectProgress = projectProgress
        )
    }

    private companion object {
        const val MAX_BATCH = 50
    }
}
