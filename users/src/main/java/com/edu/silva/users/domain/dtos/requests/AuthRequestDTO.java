package com.edu.silva.users.domain.dtos.requests;

import com.edu.silva.common.enums.AppScope;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record AuthRequestDTO(
        @Email String email,
        String password,
        String tokenGmail,
        @NotNull(message = "app is required") AppScope app) {
}
