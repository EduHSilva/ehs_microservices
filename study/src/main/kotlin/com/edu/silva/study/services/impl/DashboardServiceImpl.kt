package com.edu.silva.study.services.impl

import com.edu.silva.common.DefaultResponse
import com.edu.silva.common.PageResponse
import com.edu.silva.study.domain.dto.responses.DashboardResponseDTO
import com.edu.silva.study.domain.dto.responses.NoteResponseDTO
import com.edu.silva.study.domain.dto.responses.TaskResponseDTO
import com.edu.silva.study.domain.dto.responses.WeeklyProgress
import com.edu.silva.study.domain.entities.Notes
import com.edu.silva.study.repositories.NotesRepository
import com.edu.silva.study.services.DashboardService
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestClient
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.Date
import java.util.UUID

@Service
@Transactional(readOnly = true)
class DashboardServiceImpl(
    private val notesRepository: NotesRepository,
    @Value("\${tasks.service.url:http://localhost:8686}") tasksServiceUrl: String
) : DashboardService {
    private val tasksClient = RestClient.create(tasksServiceUrl)

    override fun get(userId: UUID): DashboardResponseDTO {
        val today = LocalDate.now()
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekEnd = weekStart.plusDays(6)
        val tasks = fetchTasks(userId, weekStart, weekEnd)

        return DashboardResponseDTO(
            weeklyProgress = calculateWeeklyProgress(tasks, weekStart, weekEnd),
            tasks = tasks.take(10),
            notes = notesRepository.findTop5ByUserIdOrderByCreatedDateDesc(userId).map { it.toResponse() }
        )
    }

    private fun fetchTasks(userId: UUID, start: LocalDate, end: LocalDate): List<TaskResponseDTO> {
        val response = tasksClient.get()
            .uri { builder ->
                builder.path("/")
                    .queryParam("page", 0)
                    .queryParam("size", 1000)
                    .queryParam("start", start.toString())
                    .queryParam("end", end.toString())
                    .build()
            }
            .header("X-User-ID", userId.toString())
            .retrieve()
            .body(object : ParameterizedTypeReference<DefaultResponse<PageResponse<TaskResponseDTO>>>() {})
            ?: error("Tasks service returned an empty response")

        return response.data?.items ?: emptyList()
    }

    private fun calculateWeeklyProgress(tasks: List<TaskResponseDTO>, start: LocalDate, end: LocalDate): WeeklyProgress {
        var total = 0
        var completed = 0
        for (task in tasks) {
            if (task.type == "DAILY") {
                val firstDay = maxOf(start, task.startDate?.toLocalDate() ?: start)
                val lastDay = minOf(end, task.endDate?.toLocalDate() ?: end)
                if (!firstDay.isAfter(lastDay)) {
                    total += (lastDay.toEpochDay() - firstDay.toEpochDay() + 1).toInt()
                    completed += task.dailyCompletions.orEmpty()
                        .mapNotNull { it.completionDate }
                        .filter { !it.isBefore(firstDay) && !it.isAfter(lastDay) }
                        .distinct()
                        .count()
                }
            } else {
                total++
                if (task.status == "DONE") completed++
            }
        }
        return WeeklyProgress(completed, total)
    }

    private fun Notes.toResponse(): NoteResponseDTO = NoteResponseDTO(
        id = id!!,
        folderId = folder?.id,
        title = title,
        content = content
    )

    private fun Date.toLocalDate(): LocalDate = toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
}
