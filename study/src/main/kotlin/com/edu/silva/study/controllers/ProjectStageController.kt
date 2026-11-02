package com.edu.silva.study.controllers

import com.edu.silva.common.DefaultResponse
import com.edu.silva.study.domain.dto.requests.ProjectStageRequestDTO
import com.edu.silva.study.domain.dto.responses.ProjectStageResponseDTO
import com.edu.silva.study.services.ProjectStageService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

private const val BASE_URL = "/project/stage"

@RestController
class ProjectStageController {

    private var service: ProjectStageService

    constructor(service: ProjectStageService) {
        this.service = service
    }

    @PostMapping(BASE_URL)
    fun save(
        @RequestBody @Valid request: @Valid ProjectStageRequestDTO,
    ): ResponseEntity<DefaultResponse<ProjectStageResponseDTO?>> {
        val stage = service.add(request)
        return ResponseEntity.ok(
            DefaultResponse<ProjectStageResponseDTO?>(
                "Project stage successfully saved",
                stage,
                BASE_URL,
                stage!!.id
            )
        )
    }

    @PutMapping("$BASE_URL/{id}")
    fun update(
        @RequestBody @Valid request: @Valid ProjectStageRequestDTO,
        @PathVariable id: UUID?
    ): ResponseEntity<DefaultResponse<ProjectStageResponseDTO?>> {
        return ResponseEntity.ok(
            DefaultResponse<ProjectStageResponseDTO?>(
                "Project stage successfully updated",
                service.update(id, request),
                BASE_URL,
                id
            )
        )
    }
}