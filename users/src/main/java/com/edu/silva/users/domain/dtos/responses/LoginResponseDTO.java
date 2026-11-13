package com.edu.silva.users.domain.dtos.responses;


import java.util.Date;
import java.util.List;
import java.util.UUID;

public record LoginResponseDTO(
        UUID id,
        String username,
        String email,
        Date lastLogin,
        String token,
        List<String> roles,
        List<String> apps) {
}
