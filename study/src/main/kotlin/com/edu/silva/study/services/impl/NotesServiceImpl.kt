package com.edu.silva.study.services.impl

import com.edu.silva.study.domain.dto.requests.NoteRequestDTO
import com.edu.silva.study.domain.dto.responses.NoteResponseDTO
import com.edu.silva.study.domain.entities.Notes
import com.edu.silva.study.infra.exceptions.EntityNotFoundException
import com.edu.silva.study.infra.exceptions.InvalidRequestException
import com.edu.silva.study.repositories.FolderRepository
import com.edu.silva.study.repositories.NotesRepository
import com.edu.silva.study.infra.config.UserContext
import com.edu.silva.study.services.NotesService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class NotesServiceImpl(
    private val repository: NotesRepository,
    private val folderRepository: FolderRepository
) : NotesService {

    override fun findAll(page: Int, size: Int): Page<NoteResponseDTO> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"))
        return repository.findAll(pageable).map { it.toResponse() }
    }

    override fun findById(id: UUID?): NoteResponseDTO {
        return findEntity(id).toResponse()
    }

    override fun add(folderId: UUID?, request: NoteRequestDTO?): NoteResponseDTO {
        if (request == null) {
            throw InvalidRequestException("Note request is required")
        }
        val folder = folderId?.let {
            folderRepository.findByIdAndUserId(it, UserContext.getUser())
                .orElseThrow { EntityNotFoundException("Folder", it) }
        }
        val note = Notes(
            title = request.title,
            content = request.content,
            folder = folder,
            userId = UserContext.getUser()
        )
        return repository.save(note).toResponse()
    }

    override fun update(id: UUID?, request: NoteRequestDTO?): NoteResponseDTO {
        if (request == null) {
            throw InvalidRequestException("Note request is required")
        }
        val note = findEntity(id)
        note.title = request.title
        note.content = request.content
        return repository.save(note).toResponse()
    }

    override fun delete(id: UUID?) {
        repository.delete(findEntity(id))
    }

    private fun findEntity(id: UUID?): Notes {
        if (id == null) {
            throw EntityNotFoundException("Note", id)
        }
        return repository.findByIdAndUserId(id, UserContext.getUser())
            .orElseThrow { EntityNotFoundException("Note", id) }
    }

    private fun Notes.toResponse(): NoteResponseDTO {
        return NoteResponseDTO(
            id = id!!,
            folderId = folder?.id,
            title = title,
            content = content
        )
    }
}
