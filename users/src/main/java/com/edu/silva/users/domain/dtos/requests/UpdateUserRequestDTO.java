package com.edu.silva.users.domain.dtos.requests;

import com.edu.silva.users.domain.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record UpdateUserRequestDTO(
        @NotBlank String username,
        @Email String email,
        List<UserRole> roles
) {
    public UpdateUserRequestDTO(String username) {
        this(username, null, null);
    }
}
