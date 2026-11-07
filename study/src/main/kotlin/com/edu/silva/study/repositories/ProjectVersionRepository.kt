package com.edu.silva.study.repositories

import com.edu.silva.study.domain.entities.ProjectVersion
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ProjectVersionRepository : JpaRepository<ProjectVersion, UUID> {
    fun findByIdAndUserId(id: UUID, userId: UUID): java.util.Optional<ProjectVersion>
}
