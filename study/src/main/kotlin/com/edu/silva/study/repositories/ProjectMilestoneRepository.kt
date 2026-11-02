package com.edu.silva.study.repositories

import com.edu.silva.study.domain.entities.ProjectMilestone
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ProjectMilestoneRepository : JpaRepository<ProjectMilestone, UUID> {
    fun findByIdAndUserId(id: UUID, userId: UUID): java.util.Optional<ProjectMilestone>
}
