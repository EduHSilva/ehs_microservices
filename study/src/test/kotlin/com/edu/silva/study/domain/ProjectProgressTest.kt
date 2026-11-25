package com.edu.silva.study.domain

import com.edu.silva.study.domain.dto.responses.TaskResponseDTO
import com.edu.silva.study.domain.entities.ProjectStage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ProjectProgressTest {

    private fun stage(done: Boolean) = ProjectStage(name = "Etapa", done = done)

    private fun task(status: String, type: String = "UNIQUE") =
        TaskResponseDTO(status = status, type = type)

    @Test
    fun `sem etapas nem tarefas o progresso e zero`() {
        assertEquals(0, ProjectProgress.percent(emptyList(), emptyList()))
    }

    @Test
    fun `tarefa pendente segura o percentual`() {
        val stages = listOf(stage(true), stage(true))
        val tasks = listOf(task("DONE"), task("PENDING"))

        assertEquals(75, ProjectProgress.percent(stages, tasks))
    }

    @Test
    fun `tarefas excluidas nao entram na conta`() {
        val stages = listOf(stage(true))
        val tasks = listOf(task("DONE"), task("DELETED"), task("DELETED"))

        assertEquals(100, ProjectProgress.percent(stages, tasks))
    }

    @Test
    fun `progresso e arredondado para o inteiro mais proximo`() {
        val oneOfThree = ProjectProgress.percent(listOf(stage(true)), listOf(task("PENDING"), task("PENDING")))
        val twoOfThree = ProjectProgress.percent(listOf(stage(true), stage(true)), listOf(task("PENDING")))

        assertEquals(33, oneOfThree)
        assertEquals(67, twoOfThree)
    }

    @Test
    fun `tarefa recorrente concluida hoje conta como concluida`() {
        val stages = listOf(stage(false))
        val tasks = listOf(task("DONE", "DAILY"))

        assertEquals(50, ProjectProgress.percent(stages, tasks))
    }

    @Test
    fun `projeto so com tarefas`() {
        val tasks = listOf(task("DONE"), task("WORKING"), task("DELAYED"))

        assertEquals(33, ProjectProgress.percent(emptyList(), tasks))
    }
}
