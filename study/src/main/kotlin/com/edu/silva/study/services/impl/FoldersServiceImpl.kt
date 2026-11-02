package com.edu.silva.study.services.impl

import com.edu.silva.study.domain.dto.requests.FolderRequestDTO
import com.edu.silva.study.domain.dto.responses.FolderResponseDTO
import com.edu.silva.study.domain.dto.responses.NoteResponseDTO
import com.edu.silva.study.domain.entities.Folder
import com.edu.silva.study.domain.entities.Notes
import com.edu.silva.study.infra.exceptions.EntityNotFoundException
import com.edu.silva.study.repositories.FolderRepository
import com.edu.silva.study.infra.config.UserContext
import com.edu.silva.study.services.FoldersService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class FoldersServiceImpl(
    private val repository: FolderRepository
) : FoldersService {

    override fun findAll(page: Int, size: Int): Page<FolderResponseDTO> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"))
        return repository.findAll(pageable).map { it.toResponse() }
    }

    override fun findById(id: UUID?): FolderResponseDTO {
        return findEntity(id).toResponse()
    }

    override fun add(request: FolderRequestDTO): FolderResponseDTO {
        val folder = Folder(name = request.name, userId = UserContext.getUser())
        return repository.save(folder).toResponse()
    }

    override fun update(id: UUID?, request: FolderRequestDTO): FolderResponseDTO {
        val folder = findEntity(id)
        folder.name = request.name
        return repository.save(folder).toResponse()
    }

    override fun delete(id: UUID?) {
        repository.delete(findEntity(id))
    }

    private fun findEntity(id: UUID?): Folder {
        if (id == null) {
            throw EntityNotFoundException("Folder", id)
        }
        return repository.findByIdAndUserId(id, UserContext.getUser())
            .orElseThrow { EntityNotFoundException("Folder", id) }
    }

    private fun Folder.toResponse(): FolderResponseDTO {
        return FolderResponseDTO(
            id = id!!,
            name = name,
            notes = notes.map { it.toResponse() }.toCollection(ArrayList())
        )
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
