package com.edu.silva.study.domain.dto.responses

import java.util.Date
import java.util.UUID

data class FocusResponseDTO(
    val id: UUID,
    val minutes: Int,
    val title: String,
    val completedAt: Date?
)
