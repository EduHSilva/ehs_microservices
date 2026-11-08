package com.edu.silva.study.domain.entities

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.util.Date
import java.util.UUID

@Entity
@Table(name = "folders")
class Folder(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(nullable = false)
    var name: String = "",

    @Column(name = "user_id")
    var userId: UUID? = null,

    @OneToMany(mappedBy = "folder", cascade = [CascadeType.ALL], orphanRemoval = true)
    var notes: MutableList<Notes> = mutableListOf(),

    @CreationTimestamp
    var createdDate: Date? = null,

    @UpdateTimestamp
    var updatedDate: Date? = null
)
