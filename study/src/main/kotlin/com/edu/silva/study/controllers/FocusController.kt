package com.edu.silva.study.controllers

import com.edu.silva.common.DefaultResponse
import com.edu.silva.common.PageResponse
import com.edu.silva.study.domain.dto.requests.FocusRequestDTO
import com.edu.silva.study.domain.dto.responses.FocusResponseDTO
import com.edu.silva.study.infra.config.RequestTimeZone
import com.edu.silva.study.services.FocusService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.Date
import java.util.UUID

private const val BASE_URL = "/focus"

@RestController
class FocusController {

    private var service: FocusService

    constructor(service: FocusService) {
        this.service = service
    }

    @GetMapping(BASE_URL)
    fun get(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): ResponseEntity<DefaultResponse<PageResponse<FocusResponseDTO?>?>> {
        val dateAtStartOfDay = Date.from(date.atStartOfDay(RequestTimeZone.get()).toInstant())
        val folders: Page<FocusResponseDTO>? = service.findAll(dateAtStartOfDay, page, size)
        return ResponseEntity.ok(
            DefaultResponse.paged<FocusResponseDTO>(
                "Find all sessions successfully",
                folders,
                BASE_URL
            )
        )
    }

    @GetMapping("$BASE_URL/{id}")
    fun one(@PathVariable id: UUID?): ResponseEntity<DefaultResponse<FocusResponseDTO?>> {
        return ResponseEntity.ok(
            DefaultResponse<FocusResponseDTO?>(
                "Focus session find successfully",
                service.findById(id),
                BASE_URL,
                id
            )
        )
    }

    @PostMapping(BASE_URL)
    fun save(
        @RequestBody @Valid request: @Valid FocusRequestDTO
    ): ResponseEntity<DefaultResponse<FocusResponseDTO?>> {
        val folder = service.add(request)
        return ResponseEntity.ok(
            DefaultResponse<FocusResponseDTO?>(
                "Focus session successfully saved",
                folder,
                BASE_URL,
                folder.id
            )
        )
    }

    @PutMapping("$BASE_URL/{id}")
    fun update(
        @RequestBody @Valid request: @Valid FocusRequestDTO,
        @PathVariable id: UUID?
    ): ResponseEntity<DefaultResponse<FocusResponseDTO?>> {
        return ResponseEntity.ok(
            DefaultResponse<FocusResponseDTO?>(
                "Focus successfully updated",
                service.update(id, request),
                BASE_URL,
                id
            )
        )
    }
}