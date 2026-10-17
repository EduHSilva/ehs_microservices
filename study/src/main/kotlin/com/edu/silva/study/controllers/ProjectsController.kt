package com.edu.silva.study.controllers

import com.edu.silva.common.DefaultResponse
import com.edu.silva.common.PageResponse
import com.edu.silva.study.domain.dto.requests.NoteRequestDTO
import com.edu.silva.study.domain.dto.requests.ProjectRequestDTO
import com.edu.silva.study.domain.dto.responses.NoteResponseDTO
import com.edu.silva.study.domain.dto.responses.ProjectResponseDTO
import com.edu.silva.study.services.ProjectService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

private const val BASE_URL = "/projects"

@RestController
class ProjectsController {

    private var service: ProjectService

    constructor(service: ProjectService) {
        this.service = service
    }

    @GetMapping(BASE_URL)
    fun get(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<DefaultResponse<PageResponse<ProjectResponseDTO?>?>> {
        val projects: Page<ProjectResponseDTO>? = service.findAll(page, size)
        return ResponseEntity.ok(
            DefaultResponse.paged<ProjectResponseDTO>(
                "Find all projects successfully",
                projects,
                BASE_URL
            )
        )
    }

    @GetMapping("$BASE_URL/{id}")
    fun one(@PathVariable id: UUID?): ResponseEntity<DefaultResponse<ProjectResponseDTO?>> {
        return ResponseEntity.ok(
            DefaultResponse<ProjectResponseDTO?>(
                "Project find successfully",
                service.findById(id),
                BASE_URL,
                id
            )
        )
    }

    @PostMapping("$BASE_URL/{id}")
    fun save(
        @RequestBody @Valid request: @Valid ProjectRequestDTO, @PathVariable id: String
    ): ResponseEntity<DefaultResponse<ProjectResponseDTO?>> {
        val note = service.add(request)
        return ResponseEntity.ok(
            DefaultResponse<ProjectResponseDTO?>(
                "Project successfully updated",
                note,
                BASE_URL,
                note!!.id
            )
        )
    }

    @PutMapping("$BASE_URL/{id}")
    fun update(
        @RequestBody @Valid request: @Valid ProjectRequestDTO,
        @PathVariable id: UUID?
    ): ResponseEntity<DefaultResponse<ProjectResponseDTO?>> {
        return ResponseEntity.ok(
            DefaultResponse<ProjectResponseDTO?>(
                "Project successfully updated",
                service.update(id, request),
                BASE_URL,
                id
            )
        )
    }
}