package com.edu.silva.study.services.impl

import com.edu.silva.study.domain.dto.requests.FocusRequestDTO
import com.edu.silva.study.domain.dto.responses.FocusResponseDTO
import com.edu.silva.study.services.FocusService
import org.springframework.data.domain.Page
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class FocusServiceImpl : FocusService{
    override fun findAll(
        page: Int,
        size: Int
    ): Page<FocusResponseDTO>? {
        TODO("Not yet implemented")
    }

    override fun findById(id: UUID?): FocusResponseDTO? {
        TODO("Not yet implemented")
    }

    override fun add(request: FocusRequestDTO): FocusResponseDTO {
        TODO("Not yet implemented")
    }

    override fun update(
        id: UUID?,
        request: FocusRequestDTO
    ): FocusResponseDTO {
        TODO("Not yet implemented")
    }

}