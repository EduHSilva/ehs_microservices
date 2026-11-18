package com.silva.edu.finances.domain.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AccountInviteActionRequest(
        @NotBlank @Size(max = 64) String token
) {}
