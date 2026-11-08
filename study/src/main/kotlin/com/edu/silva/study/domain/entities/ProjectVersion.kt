package com.edu.silva.study.domain.entities

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Column
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.util.Date
import java.util.UUID

@Entity
@Table(name = "project_versions")
class ProjectVersion(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    var project: Projects? = null,

    var title: String = "",

    @Column(name = "user_id")
    var userId: UUID? = null,

    var dueDate: Date? = null,

    @CreationTimestamp
    var createdDate: Date? = null,

    @UpdateTimestamp
    var updatedDate: Date? = null
)
