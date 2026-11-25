package com.edu.silva.study.domain

import com.edu.silva.study.domain.dto.responses.TaskResponseDTO
import com.edu.silva.study.domain.entities.ProjectStage

/**
 * Progresso do projeto considerando etapas e tarefas: cada etapa e cada tarefa valem um item.
 * Uma tarefa pendente segura o percentual mesmo com a etapa concluída. Para tarefas recorrentes
 * o serviço de tarefas já devolve `DONE` quando o dia atual está concluído, então o status basta.
 */
object ProjectProgress {
    private const val DONE = "DONE"
    private const val DELETED = "DELETED"

    fun percent(stages: List<ProjectStage>, tasks: List<TaskResponseDTO>): Int {
        val activeTasks = tasks.filter { it.status != DELETED }
        val total = stages.size + activeTasks.size
        if (total == 0) return 0
        val done = stages.count { it.done } + activeTasks.count { it.status == DONE }
        return Math.round(done.toDouble() / total * 100).toInt()
    }
}
