package com.edu.silva.study.domain.dto.requests

import com.edu.silva.common.enums.Status
import java.util.UUID

data class ProjectRequestDTO(
    val id: UUID,
    val title: String,
    val milestone: String,
    val status: Status
)
