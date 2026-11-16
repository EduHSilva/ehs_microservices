package com.edu.silva.users.domain.dtos.responses;

import com.edu.silva.users.domain.entities.User;
import com.edu.silva.users.domain.enums.UserRole;
import com.edu.silva.users.domain.enums.UserStatus;

import java.util.List;
import java.util.UUID;

/**
 * Perfil mínimo devolvido para outros serviços (ex.: fitness). Não inclui
 * senha, empresa nem qualquer dado administrativo.
 */
public record InternalUserProfileDTO(
        UUID id,
        String username,
        String email,
        List<UserRole> roles,
        UserStatus status) {

    public static InternalUserProfileDTO from(User user) {
        return new InternalUserProfileDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                List.copyOf(user.getRoles()),
                user.getStatus());
    }
}
