package com.edu.silva.study.services.impl

import com.edu.silva.study.domain.dto.requests.FocusRequestDTO
import com.edu.silva.study.domain.dto.responses.FocusResponseDTO
import com.edu.silva.study.domain.entities.Focus
import com.edu.silva.study.infra.exceptions.EntityNotFoundException
import com.edu.silva.study.repositories.FocusRepository
import com.edu.silva.study.infra.config.UserContext
import com.edu.silva.study.services.FocusService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.UUID

@Service
@Transactional
class FocusServiceImpl(
    private val repository: FocusRepository
) : FocusService {

    override fun findAll(date: Date?, page: Int, size: Int): Page<FocusResponseDTO> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "completedAt"))
        val sessions = if (date == null) {
            repository.findAll(pageable)
        } else {
            val day = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            repository.findByCompletedAtBetween(startOfDay(day), endOfDay(day), pageable)
        }
        return sessions.map { it.toResponse() }
    }

    override fun findById(id: UUID?): FocusResponseDTO {
        return findEntity(id).toResponse()
    }

    override fun add(request: FocusRequestDTO): FocusResponseDTO {
        val session = Focus(
            minutes = request.minutes,
            title = request.title,
            completedAt = Date(),
            userId = UserContext.getUser()
        )
        return repository.save(session).toResponse()
    }

    override fun update(id: UUID?, request: FocusRequestDTO): FocusResponseDTO {
        val session = findEntity(id)
        session.minutes = request.minutes
        session.title = request.title
        return repository.save(session).toResponse()
    }

    private fun findEntity(id: UUID?): Focus {
        if (id == null) {
            throw EntityNotFoundException("Focus", id)
        }
        return repository.findByIdAndUserId(id, UserContext.getUser())
            .orElseThrow { EntityNotFoundException("Focus", id) }
    }

    private fun Focus.toResponse(): FocusResponseDTO {
        return FocusResponseDTO(
            id = id!!,
            minutes = minutes,
            title = title,
            completedAt = completedAt
        )
    }

    private fun startOfDay(day: LocalDate): Date {
        return Date.from(day.atStartOfDay(ZoneId.systemDefault()).toInstant())
    }

    private fun endOfDay(day: LocalDate): Date {
        return Date.from(day.plusDays(1).atStartOfDay(ZoneId.systemDefault()).minusNanos(1).toInstant())
    }
}
