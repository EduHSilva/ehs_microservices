package com.silva.edu.finances.services;

import com.silva.edu.finances.domain.dtos.request.UpdateUserPreferenceRequestDTO;
import com.silva.edu.finances.domain.dtos.response.UserPreferenceResponseDTO;

public interface UserPreferenceService {
    UserPreferenceResponseDTO getCurrent();

    UserPreferenceResponseDTO updateCurrent(UpdateUserPreferenceRequestDTO request);
}
