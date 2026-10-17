package com.edu.silva.study.controllers

import com.edu.silva.common.DefaultResponse
import com.edu.silva.common.PageResponse
import com.edu.silva.study.domain.dto.requests.FolderRequestDTO
import com.edu.silva.study.domain.dto.responses.FolderResponseDTO
import com.edu.silva.study.services.FoldersService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

private const val BASE_URL = "/notes/folder"

@RestController
class FoldersController {

    private var service: FoldersService

    constructor(service: FoldersService) {
        this.service = service
    }

    @GetMapping(BASE_URL)
    fun get(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<DefaultResponse<PageResponse<FolderResponseDTO?>?>> {
        val folders: Page<FolderResponseDTO>? = service.findAll(page, size)
        return ResponseEntity.ok(
            DefaultResponse.paged<FolderResponseDTO>(
                "Find all folders successfully",
                folders,
                BASE_URL
            )
        )
    }

    @GetMapping("$BASE_URL/{id}")
    fun one(@PathVariable id: UUID?): ResponseEntity<DefaultResponse<FolderResponseDTO?>> {
        return ResponseEntity.ok(
            DefaultResponse<FolderResponseDTO?>(
                "Folder find successfully",
                service.findById(id),
                BASE_URL,
                id
            )
        )
    }

    @PostMapping("$BASE_URL/{id}")
    fun save(
        @RequestBody @Valid request: @Valid FolderRequestDTO, @PathVariable id: String
    ): ResponseEntity<DefaultResponse<FolderResponseDTO?>> {
        val folder = service.add(request)
        return ResponseEntity.ok(
            DefaultResponse<FolderResponseDTO?>(
                "Folder successfully updated",
                folder,
                BASE_URL,
                folder.id
            )
        )
    }

    @PutMapping("$BASE_URL/{id}")
    fun update(
        @RequestBody @Valid request: @Valid FolderRequestDTO,
        @PathVariable id: UUID?
    ): ResponseEntity<DefaultResponse<FolderResponseDTO?>> {
        return ResponseEntity.ok(
            DefaultResponse<FolderResponseDTO?>(
                "Folder successfully updated",
                service.update(id, request),
                BASE_URL,
                id
            )
        )
    }
}