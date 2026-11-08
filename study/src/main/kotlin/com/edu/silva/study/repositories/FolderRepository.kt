package com.edu.silva.study.repositories

import com.edu.silva.study.domain.entities.Folder
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface FolderRepository : JpaRepository<Folder, UUID> {
    fun findByIdAndUserId(id: UUID, userId: UUID): java.util.Optional<Folder>
    fun findAllByUserId(userId: UUID, pageable: Pageable): Page<Folder>
}
