package com.silva.edu.finances.domain.dtos.response;

import java.util.List;
import java.util.UUID;

public record AccountMembersResponse(String accountId, String accountName, UUID ownerId, String ownerName,
                                     String ownerEmail, List<AccountMemberResponse> members) {}
