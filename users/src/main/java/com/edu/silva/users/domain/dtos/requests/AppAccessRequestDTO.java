package com.edu.silva.users.domain.dtos.requests;

import com.edu.silva.common.enums.AppScope;
import com.edu.silva.users.domain.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Solicitação de acesso a uma aplicação para um e-mail já cadastrado.
 * A {@code role} é opcional quando a aplicação tem exatamente uma role.
 */
public record AppAccessRequestDTO(
        @NotBlank @Email String email,
        @NotNull AppScope app,
        UserRole role
) {
}
