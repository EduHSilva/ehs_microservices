package com.silva.edu.finances.infra.integration;

import com.edu.silva.common.dtos.EmailDTO;
import com.edu.silva.common.enums.EmailType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Publishes account invitations to the notifications queue. Delivery is best
 * effort: a broker failure does not roll back the invitation, which can be
 * resent from the members screen.
 */
@Component
public class AccountInviteNotifier {
    private static final Logger log = LoggerFactory.getLogger(AccountInviteNotifier.class);

    private final RabbitTemplate rabbitTemplate;
    private final AmqpAdmin amqpAdmin;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final String queue;
    private final String inviteUrlBase;

    public AccountInviteNotifier(RabbitTemplate rabbitTemplate, AmqpAdmin amqpAdmin, ObjectMapper objectMapper,
                                 @Value("${finances.notifications.enabled:true}") boolean enabled,
                                 @Value("${finances.notifications.queue:default.notifications}") String queue,
                                 @Value("${finances.invite.url-base:http://localhost}") String inviteUrlBase,
                                 @Value("${finances.invite.path:/app/finances/?invite=}") String invitePath) {
        this.rabbitTemplate = rabbitTemplate;
        this.amqpAdmin = amqpAdmin;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.queue = queue;
        this.inviteUrlBase = trimTrailingSlash(inviteUrlBase) + invitePath;
    }

    public void sendInvite(String email, String name, UUID userId, String ownerName, String accountName,
                           String token) {
        if (!enabled) return;
        EmailDTO dto = new EmailDTO();
        dto.setUserID(userId);
        dto.setUsername(name == null || name.isBlank() ? email : name);
        dto.setEmailTo(email);
        dto.setLocaleTag("pt-BR");
        dto.setEmailType(EmailType.FINANCES_ACCOUNT_INVITE);
        dto.setBtnURL(inviteUrlBase + token);
        dto.setVariables(Map.of("owner", ownerName, "account", accountName));
        try {
            amqpAdmin.declareQueue(new Queue(queue, true));
            byte[] body = objectMapper.writeValueAsBytes(dto);
            MessageProperties properties = new MessageProperties();
            properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            rabbitTemplate.send("", queue, new Message(body, properties));
        } catch (AmqpException ex) {
            log.warn("account invite e-mail to {} could not be published: {}", email, ex.getMessage());
        } catch (Exception ex) {
            log.warn("account invite e-mail to {} could not be published: {}", email, ex.getMessage());
        }
    }

    private static String trimTrailingSlash(String value) {
        String base = value == null ? "" : value.trim();
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base;
    }
}
