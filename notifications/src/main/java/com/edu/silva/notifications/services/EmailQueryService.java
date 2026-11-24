package com.edu.silva.notifications.services;

import com.edu.silva.notifications.domain.dtos.EmailResponseDTO;
import com.edu.silva.notifications.repositories.EmailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailQueryService {

    private final EmailRepository emailRepository;

    public Page<EmailResponseDTO> list(int page, int size) {
        Sort newestFirst = Sort.by(Sort.Direction.DESC, "sentDate");
        return emailRepository.findAll(PageRequest.of(page, size, newestFirst))
                .map(EmailResponseDTO::from);
    }
}
