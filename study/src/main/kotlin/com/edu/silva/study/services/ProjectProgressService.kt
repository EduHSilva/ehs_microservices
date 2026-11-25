package com.edu.silva.study.services

import com.edu.silva.study.domain.entities.ProjectStage
import com.edu.silva.study.domain.entities.Projects
import java.util.UUID

interface ProjectProgressService {
    /** Progresso dos projetos informados, numa única consulta ao serviço de tarefas. */
    fun progressByProject(projects: List<Projects>): Map<UUID, Int>

    /** Progresso do projeto usando as etapas informadas (resposta de etapa recém-salva). */
    fun progressOf(projectId: UUID, stages: List<ProjectStage>): Int
}
