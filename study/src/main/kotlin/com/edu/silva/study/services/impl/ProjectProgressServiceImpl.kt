package com.edu.silva.study.services.impl

import com.edu.silva.study.domain.ProjectProgress
import com.edu.silva.study.domain.dto.responses.TaskResponseDTO
import com.edu.silva.study.domain.entities.ProjectStage
import com.edu.silva.study.domain.entities.Projects
import com.edu.silva.study.infra.config.RequestTimeZone
import com.edu.silva.study.infra.config.UserContext
import com.edu.silva.study.infra.tasks.TasksClient
import com.edu.silva.study.services.ProjectProgressService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
@Transactional(readOnly = true)
class ProjectProgressServiceImpl(
    private val tasksClient: TasksClient
) : ProjectProgressService {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun progressByProject(projects: List<Projects>): Map<UUID, Int> {
        if (projects.isEmpty()) return emptyMap()
        val byProject = fetchTasks(null)
            .filter { it.status != DELETED }
            .groupBy { it.projectId }
        return projects.associate { project ->
            project.id!! to ProjectProgress.percent(project.stages, byProject[project.id].orEmpty())
        }
    }

    override fun progressOf(projectId: UUID, stages: List<ProjectStage>): Int {
        val tasks = fetchTasks(projectId).filter { it.status != DELETED && it.projectId == projectId }
        return ProjectProgress.percent(stages, tasks)
    }

    // Mesma janela larga usada pelos clientes: as tarefas do projeto podem estar em qualquer data.
    // Se o serviço de tarefas falhar, o progresso cai para as etapas e não derruba a listagem.
    private fun fetchTasks(projectId: UUID?): List<TaskResponseDTO> {
        val today = RequestTimeZone.today()
        val start = LocalDate.of(today.year - 2, 1, 1)
        val end = LocalDate.of(today.year + 2, 12, 31)
        return try {
            tasksClient.tasksForUser(UserContext.getUser(), start, end, RequestTimeZone.get(), projectId)
        } catch (e: Exception) {
            log.warn("Could not load tasks for project progress, using stages only: {}", e.message)
            emptyList()
        }
    }

    private companion object {
        const val DELETED = "DELETED"
    }
}
