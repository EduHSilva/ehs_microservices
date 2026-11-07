package com.edu.silva.study.controllers

import com.edu.silva.common.DefaultResponse
import com.edu.silva.study.domain.dto.requests.ProjectVersionRequestDTO
import com.edu.silva.study.domain.dto.responses.ProjectVersionResponseDTO
import com.edu.silva.study.services.ProjectVersionService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

private const val BASE_URL = "/project/version"

@RestController
class ProjectVersionController {

    private var service: ProjectVersionService

    constructor(service: ProjectVersionService) {
        this.service = service
    }

    @PostMapping(BASE_URL)
    fun save(
        @RequestBody @Valid request: @Valid ProjectVersionRequestDTO,
    ): ResponseEntity<DefaultResponse<ProjectVersionResponseDTO?>> {
        val version = service.add(request)
        return ResponseEntity.ok(
            DefaultResponse<ProjectVersionResponseDTO?>(
                "Project version successfully saved",
                version,
                BASE_URL,
                version!!.id
            )
        )
    }

    @PutMapping("$BASE_URL/{id}")
    fun update(
        @RequestBody @Valid request: @Valid ProjectVersionRequestDTO,
        @PathVariable id: UUID?
    ): ResponseEntity<DefaultResponse<ProjectVersionResponseDTO?>> {
        return ResponseEntity.ok(
            DefaultResponse<ProjectVersionResponseDTO?>(
                "Project version successfully updated",
                service.update(id, request),
                BASE_URL,
                id
            )
        )
    }
}
