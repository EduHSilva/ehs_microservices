package com.silva.edu.finances.services.impl;

import com.silva.edu.finances.domain.dtos.request.UpdateUserPreferenceRequestDTO;
import com.silva.edu.finances.domain.dtos.response.UserPreferenceResponseDTO;
import com.silva.edu.finances.domain.entities.UserPreference;
import com.silva.edu.finances.infra.config.UserContext;
import com.silva.edu.finances.repositories.UserPreferenceRepository;
import com.silva.edu.finances.services.UserPreferenceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserPreferenceServiceImpl implements UserPreferenceService {
    private final UserPreferenceRepository repository;

    public UserPreferenceServiceImpl(UserPreferenceRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public UserPreferenceResponseDTO getCurrent() {
        UserPreference preferences = repository.findById(UserContext.getUser())
                .orElseGet(() -> new UserPreference(UserContext.getUser(), true, "BRL"));
        return toResponse(preferences);
    }

    @Override
    @Transactional
    public UserPreferenceResponseDTO updateCurrent(UpdateUserPreferenceRequestDTO request) {
        UserPreference preferences = repository.findById(UserContext.getUser())
                .orElseGet(() -> new UserPreference(UserContext.getUser(), true, "BRL"));
        preferences.setShowBusinessAccount(request.showBusinessAccount());
        preferences.setCurrency(request.currency());
        return toResponse(repository.save(preferences));
    }

    private UserPreferenceResponseDTO toResponse(UserPreference preferences) {
        return new UserPreferenceResponseDTO(preferences.isShowBusinessAccount(), preferences.getCurrency());
    }
}
