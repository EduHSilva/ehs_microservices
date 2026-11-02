package com.edu.silva.study.services

import com.edu.silva.study.domain.dto.responses.DashboardResponseDTO
import java.util.UUID

interface DashboardService {
    fun get(userId: UUID): DashboardResponseDTO
}
