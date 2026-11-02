package com.edu.silva.study.repositories

import com.edu.silva.study.domain.entities.Projects
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ProjectRepository : JpaRepository<Projects, UUID> {
    fun findByIdAndUserId(id: UUID, userId: UUID): java.util.Optional<Projects>
}
