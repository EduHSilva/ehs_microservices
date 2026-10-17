package com.edu.silva.study.services.impl

import com.edu.silva.study.domain.dto.requests.NoteRequestDTO
import com.edu.silva.study.domain.dto.responses.NoteResponseDTO
import com.edu.silva.study.services.NotesService
import org.springframework.data.domain.Page
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class NotesServiceImpl : NotesService {
    override fun findAll(
        page: Int,
        size: Int
    ): Page<NoteResponseDTO>? {
        TODO("Not yet implemented")
    }

    override fun findById(id: UUID?): NoteResponseDTO? {
        TODO("Not yet implemented")
    }

    override fun add(request: NoteRequestDTO?): NoteResponseDTO? {
        TODO("Not yet implemented")
    }

    override fun update(
        id: UUID?,
        request: NoteRequestDTO?
    ): NoteResponseDTO? {
        TODO("Not yet implemented")
    }


}