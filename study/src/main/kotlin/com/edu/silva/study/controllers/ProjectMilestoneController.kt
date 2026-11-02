package com.edu.silva.study.controllers

import com.edu.silva.common.DefaultResponse
import com.edu.silva.study.domain.dto.requests.ProjectMilestoneRequestDTO
import com.edu.silva.study.domain.dto.requests.ProjectStageRequestDTO
import com.edu.silva.study.domain.dto.responses.ProjectMilestoneResponseDTO
import com.edu.silva.study.domain.dto.responses.ProjectStageResponseDTO
import com.edu.silva.study.services.ProjectMilestoneService
import com.edu.silva.study.services.ProjectStageService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

private const val BASE_URL = "/project/milestone"

@RestController
class ProjectMilestoneController {

    private var service: ProjectMilestoneService

    constructor(service: ProjectMilestoneService) {
        this.service = service
    }

    @PostMapping(BASE_URL)
    fun save(
        @RequestBody @Valid request: @Valid ProjectMilestoneRequestDTO,
    ): ResponseEntity<DefaultResponse<ProjectMilestoneResponseDTO?>> {
        val milestone = service.add(request)
        return ResponseEntity.ok(
            DefaultResponse<ProjectMilestoneResponseDTO?>(
                "Project stage successfully saved",
                milestone,
                BASE_URL,
                milestone!!.id
            )
        )
    }

    @PutMapping("$BASE_URL/{id}")
    fun update(
        @RequestBody @Valid request: @Valid ProjectMilestoneRequestDTO,
        @PathVariable id: UUID?
    ): ResponseEntity<DefaultResponse<ProjectMilestoneResponseDTO?>> {
        return ResponseEntity.ok(
            DefaultResponse<ProjectMilestoneResponseDTO?>(
                "Project stage successfully updated",
                service.update(id, request),
                BASE_URL,
                id
            )
        )
    }
}