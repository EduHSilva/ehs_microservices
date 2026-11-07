package com.edu.silva.users.services;

import com.edu.silva.users.domain.dtos.requests.AuthRequestDTO;
import com.edu.silva.users.domain.dtos.requests.RegisterRequestDTO;
import com.edu.silva.users.domain.dtos.requests.UpdateUserRequestDTO;
import com.edu.silva.users.domain.dtos.responses.LoginResponseDTO;
import com.edu.silva.users.domain.dtos.responses.UserResponseDTO;
import com.edu.silva.users.domain.dtos.responses.UserStatsResponseDTO;
import com.edu.silva.users.domain.enums.UserRole;
import com.edu.silva.users.domain.enums.UserStatus;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import lombok.NonNull;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.UUID;

public interface UserService {
    UserResponseDTO save(RegisterRequestDTO request);

    void delete(UUID id);

    Page<@NonNull UserResponseDTO> findAll(int page, int size);

    Page<@NonNull UserResponseDTO> search(String q, UserStatus status, UserRole role, int page, int size, String sort, String direction);

    String exportCsv(String q, UserStatus status, UserRole role);

    UserStatsResponseDTO stats(LocalDate from, LocalDate to, String interval);

    UserResponseDTO findById(UUID id);

    UserResponseDTO update(UUID id, UpdateUserRequestDTO request);

    UserResponseDTO updateStatus(UUID id, UserStatus status);

    void resendConfirmation(UUID id);

    void invalidateSessions(UUID id);

    UserResponseDTO confirm(UUID id);

    LoginResponseDTO login(AuthRequestDTO dto);
}
