package com.edu.silva.study.controllers

import com.edu.silva.common.DefaultResponse
import com.edu.silva.study.domain.dto.responses.DashboardResponseDTO
import com.edu.silva.study.services.DashboardService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestHeader
import java.util.UUID

private const val BASE_URL = "/dashboard"

@RestController
class DashboardController {

    private var service: DashboardService

    constructor(service: DashboardService) {
        this.service = service
    }


    @GetMapping(BASE_URL)
    fun get(@RequestHeader("X-User-ID") userId: UUID): ResponseEntity<DefaultResponse<DashboardResponseDTO>> {
        val data = service.get(userId)
        return ResponseEntity.ok(
            DefaultResponse<DashboardResponseDTO>(
                "Find all data successfully",
                data
            )
        )
    }
}