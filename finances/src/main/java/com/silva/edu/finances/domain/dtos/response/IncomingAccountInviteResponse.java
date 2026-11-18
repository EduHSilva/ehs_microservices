package com.silva.edu.finances.domain.dtos.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record IncomingAccountInviteResponse(UUID id, String accountId, String accountName, UUID ownerId,
                                            String ownerName, String ownerEmail, String email,
                                            LocalDateTime expiresAt) {}
