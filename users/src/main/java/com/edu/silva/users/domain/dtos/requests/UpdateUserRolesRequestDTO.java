package com.edu.silva.users.domain.dtos.requests;

import com.edu.silva.users.domain.enums.UserRole;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Adiciona roles a um usuário sem substituir as existentes.
 */
public record UpdateUserRolesRequestDTO(@NotEmpty List<UserRole> roles) {
}
