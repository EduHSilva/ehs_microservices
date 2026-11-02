package com.edu.silva.study.repositories

import com.edu.silva.study.domain.entities.ProjectStage
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ProjectStageRepository : JpaRepository<ProjectStage, UUID> {
    fun findByIdAndUserId(id: UUID, userId: UUID): java.util.Optional<ProjectStage>
    fun findAllByOrderByCreatedDateDesc(): List<ProjectStage>
}
