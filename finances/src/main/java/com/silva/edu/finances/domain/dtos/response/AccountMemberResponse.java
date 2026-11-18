package com.silva.edu.finances.domain.dtos.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record AccountMemberResponse(UUID id, String accountId, String email, String name, UUID userId,
                                    String status, LocalDateTime expiresAt, LocalDateTime acceptedAt,
                                    LocalDateTime createdAt, String token) {}
