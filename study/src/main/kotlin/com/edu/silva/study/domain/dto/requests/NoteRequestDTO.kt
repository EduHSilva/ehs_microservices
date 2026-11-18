package com.edu.silva.study.domain.dto.requests

import java.util.UUID

data class NoteRequestDTO (
    val title: String,
    val content: String,
    // Opcional: quando presente, move a nota para a pasta informada.
    val folderId: UUID? = null
)