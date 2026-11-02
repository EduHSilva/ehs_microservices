package com.edu.silva.study.domain.dto.responses

import java.util.UUID

data class NoteResponseDTO (
    val id: UUID,
    val folderId: UUID?,
    val title: String,
    val content: String
)