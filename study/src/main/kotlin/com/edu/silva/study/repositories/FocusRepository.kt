package com.edu.silva.study.repositories

import com.edu.silva.study.domain.entities.Focus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Date
import java.util.UUID

interface FocusRepository : JpaRepository<Focus, UUID> {
    fun findByIdAndUserId(id: UUID, userId: UUID): java.util.Optional<Focus>
    fun findByCompletedAtBetween(start: Date, end: Date, pageable: Pageable): Page<Focus>
}
