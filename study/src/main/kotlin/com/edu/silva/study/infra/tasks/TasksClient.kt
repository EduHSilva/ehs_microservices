package com.edu.silva.study.infra.tasks

import com.edu.silva.common.DefaultResponse
import com.edu.silva.common.PageResponse
import com.edu.silva.study.domain.dto.responses.TaskResponseDTO
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/**
 * Leitura das tarefas do usuário no serviço de tarefas. O progresso do projeto considera as
 * tarefas, mas os dois serviços têm bancos separados, então a consulta é por HTTP com o mesmo
 * usuário e fuso do request original.
 */
@Component
class TasksClient(
    @Value("\${tasks.service.url:http://localhost:8686}") tasksServiceUrl: String
) {
    private val client = RestClient.create(tasksServiceUrl)
    private val log = LoggerFactory.getLogger(javaClass)

    fun tasksForUser(
        userId: UUID,
        start: LocalDate,
        end: LocalDate,
        zone: ZoneId,
        projectId: UUID? = null
    ): List<TaskResponseDTO> {
        val collected = mutableListOf<TaskResponseDTO>()
        var page = 0
        while (page < MAX_PAGES) {
            val response = client.get()
                .uri { builder ->
                    builder.path("/")
                        .queryParam("page", page)
                        .queryParam("size", PAGE_SIZE)
                        .queryParam("start", start.toString())
                        .queryParam("end", end.toString())
                        .apply { if (projectId != null) queryParam("projectId", projectId.toString()) }
                        .build()
                }
                .header("X-User-ID", userId.toString())
                .header("X-Time-Zone", zone.id)
                .retrieve()
                .body(object : ParameterizedTypeReference<DefaultResponse<PageResponse<TaskResponseDTO>>>() {})
                ?: error("Tasks service returned an empty response")

            val items = response.data?.items.orEmpty()
            collected += items
            val total = response.data?.total ?: 0
            if (items.isEmpty() || collected.size.toLong() >= total) return collected
            page++
        }
        log.warn("Tasks list truncated at {} pages while computing project progress", MAX_PAGES)
        return collected
    }

    private companion object {
        const val PAGE_SIZE = 200
        const val MAX_PAGES = 25
    }
}
