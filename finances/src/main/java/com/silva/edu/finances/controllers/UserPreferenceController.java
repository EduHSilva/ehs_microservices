package com.silva.edu.finances.controllers;

import com.edu.silva.common.DefaultResponse;
import com.silva.edu.finances.domain.dtos.request.UpdateUserPreferenceRequestDTO;
import com.silva.edu.finances.domain.dtos.response.UserPreferenceResponseDTO;
import com.silva.edu.finances.services.UserPreferenceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserPreferenceController {
    private static final String BASE_URL = "/preferences";
    private final UserPreferenceService service;

    public UserPreferenceController(UserPreferenceService service) {
        this.service = service;
    }

    @GetMapping(BASE_URL)
    public ResponseEntity<DefaultResponse<UserPreferenceResponseDTO>> getCurrent() {
        return ResponseEntity.ok(new DefaultResponse<>("Preferences found successfully", service.getCurrent()));
    }

    @PutMapping(BASE_URL)
    public ResponseEntity<DefaultResponse<UserPreferenceResponseDTO>> update(
            @RequestBody @Valid UpdateUserPreferenceRequestDTO request
    ) {
        return ResponseEntity.ok(new DefaultResponse<>("Preferences updated successfully", service.updateCurrent(request)));
    }
}
