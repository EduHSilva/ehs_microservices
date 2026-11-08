package com.edu.silva.study.repositories

import com.edu.silva.study.domain.entities.Notes
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface NotesRepository : JpaRepository<Notes, UUID> {
    fun findByIdAndUserId(id: UUID, userId: UUID): java.util.Optional<Notes>
    fun findAllByUserId(userId: UUID, pageable: Pageable): Page<Notes>
    fun findTop5ByUserIdOrderByCreatedDateDesc(userId: UUID): List<Notes>
}
