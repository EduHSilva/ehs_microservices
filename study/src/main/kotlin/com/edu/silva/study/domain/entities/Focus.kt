package com.edu.silva.study.domain.entities

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.util.UUID

@Entity
class Focus(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null

)
