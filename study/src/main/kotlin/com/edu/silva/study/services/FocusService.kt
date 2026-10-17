package com.edu.silva.study.services

import com.edu.silva.study.domain.dto.requests.FocusRequestDTO
import com.edu.silva.study.domain.dto.requests.FolderRequestDTO
import com.edu.silva.study.domain.dto.responses.FocusResponseDTO
import com.edu.silva.study.domain.dto.responses.FolderResponseDTO
import org.springframework.data.domain.Page
import java.util.*

interface FocusService {
    fun findAll(page: Int, size: Int): Page<FocusResponseDTO>?

    fun findById(id: UUID?): FocusResponseDTO?

    fun add(request: FocusRequestDTO): FocusResponseDTO

    fun update(id: UUID?, request: FocusRequestDTO): FocusResponseDTO
}
