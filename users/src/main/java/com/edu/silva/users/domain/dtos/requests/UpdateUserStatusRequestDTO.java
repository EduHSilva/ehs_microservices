package com.edu.silva.users.domain.dtos.requests;

import com.edu.silva.users.domain.enums.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequestDTO(
        @NotNull UserStatus status
) {
}
