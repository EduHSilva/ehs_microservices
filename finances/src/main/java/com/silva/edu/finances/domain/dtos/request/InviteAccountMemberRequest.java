package com.silva.edu.finances.domain.dtos.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InviteAccountMemberRequest(
        @NotBlank @Email @Size(max = 255) String email
) {}
