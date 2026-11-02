package com.edu.silva.study.domain.entities

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Column
import org.hibernate.annotations.Filter
import org.hibernate.annotations.FilterDef
import org.hibernate.annotations.ParamDef
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.util.Date
import java.util.UUID

@Entity
@Table(name = "focus_sessions")
@FilterDef(name = "userFilter", parameters = [ParamDef(name = "userId", type = UUID::class)])
@Filter(name = "userFilter", condition = "user_id = :userId")
class Focus(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    var minutes: Int = 0,

    var title: String = "",

    @Column(name = "user_id")
    var userId: UUID? = null,

    var completedAt: Date? = null,

    @CreationTimestamp
    var createdDate: Date? = null,

    @UpdateTimestamp
    var updatedDate: Date? = null
)
