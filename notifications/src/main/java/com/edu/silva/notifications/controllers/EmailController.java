package com.edu.silva.notifications.controllers;

import com.edu.silva.common.DefaultResponse;
import com.edu.silva.common.PageResponse;
import com.edu.silva.notifications.domain.dtos.EmailResponseDTO;
import com.edu.silva.notifications.services.EmailQueryService;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/emails")
public class EmailController {

    private static final String BASE_URL = "/admin/emails";

    private final EmailQueryService service;

    public EmailController(EmailQueryService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<@NonNull DefaultResponse<PageResponse<EmailResponseDTO>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<@NonNull EmailResponseDTO> emails = service.list(page, size);
        return ResponseEntity.ok(DefaultResponse.paged("Emails successfully retrieved", emails, BASE_URL));
    }
}
