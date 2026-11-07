package com.edu.silva.notifications.services;

import com.edu.silva.notifications.domain.dtos.QueueStatsDTO;
import com.edu.silva.notifications.domain.entities.Email;
import com.edu.silva.notifications.domain.enums.StatusEmail;
import com.edu.silva.notifications.repositories.EmailRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Properties;

@Service
public class QueueStatsService {

    private static final Logger log = LoggerFactory.getLogger(QueueStatsService.class);

    private final AmqpAdmin amqpAdmin;
    private final EmailRepository emailRepository;

    @Value("${broker.queue.email.name}")
    private String queueName;

    public QueueStatsService(AmqpAdmin amqpAdmin, EmailRepository emailRepository) {
        this.amqpAdmin = amqpAdmin;
        this.emailRepository = emailRepository;
    }

    public QueueStatsDTO stats() {
        long ready = 0;
        long consumers = 0;

        try {
            Properties properties = amqpAdmin.getQueueProperties(queueName);

            if (properties != null) {
                ready = asLong(properties.get("MESSAGE_COUNT"));
                consumers = asLong(properties.get("CONSUMER_COUNT"));
            }
        } catch (Exception ex) {
            log.warn("Could not read queue {} properties", queueName, ex);
        }

        Optional<Email> lastError = emailRepository.findFirstByStatusOrderBySentDateDesc(StatusEmail.ERROR);

        return new QueueStatsDTO(
                queueName,
                ready,
                consumers,
                emailRepository.countByStatus(StatusEmail.SENT),
                emailRepository.countByStatus(StatusEmail.ERROR),
                lastError.map(Email::getSentDate).orElse(null),
                lastError.map(Email::getEmailTo).orElse(null)
        );
    }

    private long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0;
    }
}
