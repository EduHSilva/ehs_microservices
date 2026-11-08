package com.edu.silva.study.domain.entities

import com.edu.silva.study.domain.enums.ProjectStatus
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.Column
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.util.Date
import java.util.UUID

@Entity
@Table(name = "projects")
class Projects(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    var title: String = "",

    @Enumerated(EnumType.STRING)
    var status: ProjectStatus = ProjectStatus.WAITING,

    var advisor: String? = null,

    @Column(name = "user_id")
    var userId: UUID? = null,

    @OneToMany(mappedBy = "project", cascade = [CascadeType.ALL], orphanRemoval = true)
    var versions: MutableList<ProjectVersion> = mutableListOf(),

    @OneToMany(mappedBy = "project", cascade = [CascadeType.ALL], orphanRemoval = true)
    var stages: MutableList<ProjectStage> = mutableListOf(),

    @CreationTimestamp
    var createdDate: Date? = null,

    @UpdateTimestamp
    var updatedDate: Date? = null
)
