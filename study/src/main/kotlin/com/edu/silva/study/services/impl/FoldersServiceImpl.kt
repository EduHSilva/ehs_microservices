package com.edu.silva.study.services.impl

import com.edu.silva.study.domain.dto.requests.FolderRequestDTO
import com.edu.silva.study.domain.dto.responses.FolderResponseDTO
import com.edu.silva.study.services.FoldersService
import org.springframework.data.domain.Page
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class FoldersServiceImpl : FoldersService {
    override fun findAll(
        page: Int,
        size: Int
    ): Page<FolderResponseDTO>? {
        TODO("Not yet implemented")
    }

    override fun findById(id: UUID?): FolderResponseDTO? {
        TODO("Not yet implemented")
    }

    override fun add(request: FolderRequestDTO): FolderResponseDTO {
        TODO("Not yet implemented")
    }

    override fun update(
        id: UUID?,
        request: FolderRequestDTO
    ): FolderResponseDTO {
        TODO("Not yet implemented")
    }

}