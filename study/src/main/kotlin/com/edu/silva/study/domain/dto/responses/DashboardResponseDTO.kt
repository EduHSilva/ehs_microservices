package com.edu.silva.study.domain.dto.responses

import java.time.LocalDate
import java.util.Date
import java.util.UUID

data class DashboardResponseDTO(
    val weeklyProgress: WeeklyProgress,
    val tasks: List<TaskResponseDTO>,
    val notes: List<NoteResponseDTO>,
)

data class WeeklyProgress(
    val completed: Int,
    val total: Int,
)

data class TaskResponseDTO(
    val id: UUID? = null,
    val createdDate: Date? = null,
    val title: String? = null,
    val startDate: Date? = null,
    val endDate: Date? = null,
    val type: String? = null,
    val status: String? = null,
    val dailyCompletions: List<DailyTaskCompletionDTO>? = null,
)

data class DailyTaskCompletionDTO(val completionDate: LocalDate?)
