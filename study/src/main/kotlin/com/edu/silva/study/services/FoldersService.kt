package com.edu.silva.study.services

import com.edu.silva.study.domain.dto.requests.FolderRequestDTO
import com.edu.silva.study.domain.dto.responses.FolderResponseDTO
import org.springframework.data.domain.Page
import java.util.*

interface FoldersService {
    fun findAll(page: Int, size: Int): Page<FolderResponseDTO>?

    fun findById(id: UUID?): FolderResponseDTO?

    fun add(request: FolderRequestDTO): FolderResponseDTO

    fun update(id: UUID?, request: FolderRequestDTO): FolderResponseDTO

    fun delete(id: UUID?)
}
