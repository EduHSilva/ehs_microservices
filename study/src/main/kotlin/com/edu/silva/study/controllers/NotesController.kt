package com.edu.silva.study.controllers

import com.edu.silva.common.DefaultResponse
import com.edu.silva.common.PageResponse
import com.edu.silva.study.domain.dto.requests.NoteRequestDTO
import com.edu.silva.study.domain.dto.responses.NoteResponseDTO
import com.edu.silva.study.services.NotesService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

private const val BASE_URL = "/notes"

@RestController
class NotesController {

    private var service: NotesService

    constructor(service: NotesService) {
        this.service = service
    }

    @GetMapping(BASE_URL)
    fun get(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<DefaultResponse<PageResponse<NoteResponseDTO?>?>> {
        val folders: Page<NoteResponseDTO>? = service.findAll(page, size)
        return ResponseEntity.ok(
            DefaultResponse.paged<NoteResponseDTO>(
                "Find all notes successfully",
                folders,
                BASE_URL
            )
        )
    }

    @GetMapping("$BASE_URL/{id}")
    fun one(@PathVariable id: UUID?): ResponseEntity<DefaultResponse<NoteResponseDTO?>> {
        return ResponseEntity.ok(
            DefaultResponse<NoteResponseDTO?>(
                "Note find successfully",
                service.findById(id),
                BASE_URL,
                id
            )
        )
    }

    @PostMapping("$BASE_URL/{id}")
    fun save(
        @RequestBody @Valid request: @Valid NoteRequestDTO, @PathVariable id: String
    ): ResponseEntity<DefaultResponse<NoteResponseDTO?>> {
        val note = service.add(request)
        return ResponseEntity.ok(
            DefaultResponse<NoteResponseDTO?>(
                "Note successfully updated",
                note,
                BASE_URL,
                note!!.id
            )
        )
    }

    @PutMapping("$BASE_URL/{id}")
    fun update(
        @RequestBody @Valid request: @Valid NoteRequestDTO,
        @PathVariable id: UUID?
    ): ResponseEntity<DefaultResponse<NoteResponseDTO?>> {
        return ResponseEntity.ok(
            DefaultResponse<NoteResponseDTO?>(
                "Note successfully updated",
                service.update(id, request),
                BASE_URL,
                id
            )
        )
    }
}