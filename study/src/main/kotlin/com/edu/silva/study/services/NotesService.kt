package com.edu.silva.study.services

import com.edu.silva.study.domain.dto.requests.NoteRequestDTO
import com.edu.silva.study.domain.dto.responses.NoteResponseDTO
import org.springframework.data.domain.Page
import java.util.*

interface NotesService {
    fun findAll(page: Int, size: Int): Page<NoteResponseDTO>?

    fun findById(id: UUID?): NoteResponseDTO?

    fun add(request: NoteRequestDTO?): NoteResponseDTO?

    fun update(id: UUID?, request: NoteRequestDTO?): NoteResponseDTO?
}
