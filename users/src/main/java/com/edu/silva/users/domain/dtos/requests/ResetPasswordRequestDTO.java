package com.edu.silva.users.domain.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequestDTO(
        @NotBlank String token,
        @NotBlank @Size(min = 8, message = "A senha deve ter ao menos 8 caracteres") String newPassword) {
}
