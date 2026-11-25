package com.edu.silva.study.services.impl

import com.edu.silva.study.domain.dto.requests.ProjectRequestDTO
import com.edu.silva.study.domain.dto.responses.ProjectVersionResponseDTO
import com.edu.silva.study.domain.dto.responses.ProjectResponseDTO
import com.edu.silva.study.domain.dto.responses.ProjectStageResponseDTO
import com.edu.silva.study.domain.entities.ProjectVersion
import com.edu.silva.study.domain.entities.ProjectStage
import com.edu.silva.study.domain.entities.Projects
import com.edu.silva.study.domain.enums.ProjectStatus
import com.edu.silva.study.infra.exceptions.EntityNotFoundException
import com.edu.silva.study.infra.exceptions.InvalidRequestException
import com.edu.silva.study.repositories.ProjectRepository
import com.edu.silva.study.infra.config.UserContext
import com.edu.silva.study.services.ProjectProgressService
import com.edu.silva.study.services.ProjectService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Date
import java.util.UUID

@Service
@Transactional
class ProjectServiceImpl(
    private val repository: ProjectRepository,
    private val progressService: ProjectProgressService
) : ProjectService {

    override fun findAll(page: Int, size: Int): Page<ProjectResponseDTO> {
        // Waiting first, then in progress and finished at the end; each group alphabetical.
        val ordered = repository.findAllByUserId(UserContext.getUser(), Sort.by(Sort.Direction.ASC, "title"))
            .sortedWith(
                compareBy<Projects>(
                    { when (it.status) { ProjectStatus.WAITING -> 0; ProjectStatus.WORKING -> 1; ProjectStatus.DONE -> 2 } },
                    { it.title.lowercase() }
                )
            )
        val pageable = PageRequest.of(page, size)
        val from = minOf(page * size, ordered.size)
        val to = minOf(from + size, ordered.size)
        val items = ordered.subList(from, to)
        // Uma consulta de tarefas por page; o progresso vem do back para o front só exibir.
        val progress = progressService.progressByProject(items)
        return PageImpl(items.map { it.toResponse(progress[it.id!!] ?: 0) }, pageable, ordered.size.toLong())
    }

    override fun findById(id: UUID?): ProjectResponseDTO {
        val project = findEntity(id)
        return project.toResponse(progressOf(project))
    }

    override fun add(request: ProjectRequestDTO?): ProjectResponseDTO {
        if (request == null) {
            throw InvalidRequestException("Project request is required")
        }
        val project = Projects(
            title = request.title,
            status = request.status ?: ProjectStatus.WAITING,
            advisor = request.advisor,
            userId = UserContext.getUser()
        )
        // Projeto recém-criado ainda não tem etapas nem tarefas.
        return repository.save(project).toResponse(0)
    }

    override fun update(id: UUID?, request: ProjectRequestDTO?): ProjectResponseDTO {
        if (request == null) {
            throw InvalidRequestException("Project request is required")
        }
        val project = findEntity(id)
        project.title = request.title
        request.status?.let { project.status = it }
        // Null means "clear the advisor", which is a valid edit (the front sends it explicitly).
        project.advisor = request.advisor
        val saved = repository.save(project)
        return saved.toResponse(progressOf(saved))
    }

    override fun delete(id: UUID?) {
        repository.delete(findEntity(id))
    }

    private fun findEntity(id: UUID?): Projects {
        if (id == null) {
            throw EntityNotFoundException("Project", id)
        }
        return repository.findByIdAndUserId(id, UserContext.getUser())
            .orElseThrow { EntityNotFoundException("Project", id) }
    }

    private fun progressOf(project: Projects): Int =
        progressService.progressByProject(listOf(project))[project.id!!] ?: 0

    private fun Projects.toResponse(progress: Int): ProjectResponseDTO {
        val stages = stages.sortedBy { it.createdDate }
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
