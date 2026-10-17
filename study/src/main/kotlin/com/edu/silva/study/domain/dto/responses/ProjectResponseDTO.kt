package com.edu.silva.study.domain.dto.responses

import com.edu.silva.common.enums.Status
import java.util.UUID

data class ProjectResponseDTO(
    val id: UUID,
    val title: String,
    val milestone: String,
    val status: Status
)
