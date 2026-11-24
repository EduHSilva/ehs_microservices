package com.edu.silva.notifications.domain.dtos;

import com.edu.silva.notifications.domain.entities.Email;
import com.edu.silva.notifications.domain.enums.StatusEmail;

import java.time.LocalDateTime;
import java.util.UUID;

public record EmailResponseDTO(
        UUID id,
        UUID userID,
        String emailFrom,
        String emailTo,
        String subject,
        LocalDateTime sentDate,
        StatusEmail status
) {
    public static EmailResponseDTO from(Email email) {
        return new EmailResponseDTO(
                email.getId(),
                email.getUserID(),
                email.getEmailFrom(),
                email.getEmailTo(),
                email.getSubject(),
                email.getSentDate(),
                email.getStatus()
        );
    }
}
