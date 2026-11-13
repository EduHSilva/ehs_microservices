package com.edu.silva.users.domain.producers;

import com.edu.silva.common.dtos.EmailDTO;
import com.edu.silva.common.enums.EmailType;
import com.edu.silva.users.domain.entities.User;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class UserProducer {
    final RabbitTemplate rabbitTemplate;

    public UserProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Value(value = "${broker.queue.email.name}")
    private String routingKey;

    @Value(value = "${server.url}")
    private String url;

    @Value(value = "${app.access.path:/user/app-access}")
    private String appAccessPath;

    @Value(value = "${password.reset.path:/user/reset-password}")
    private String passwordResetPath;

    public void publishMessageEmail(User user) {
        EmailDTO emailDto = baseEmail(user, EmailType.NEW_USER);
        emailDto.setBtnURL(url + "/user/confirm/" + user.getId());
        rabbitTemplate.convertAndSend("", routingKey, emailDto);
    }

    public void publishAppAccessEmail(User user, String app, String role, String token) {
        EmailDTO emailDto = baseEmail(user, EmailType.APP_ACCESS);
        emailDto.setBtnURL(url + appAccessPath + "?token=" + token);
        emailDto.setVariables(Map.of("app", app, "role", role));
        rabbitTemplate.convertAndSend("", routingKey, emailDto);
    }

    public void publishPasswordResetEmail(User user, String token) {
        EmailDTO emailDto = baseEmail(user, EmailType.PASSWORD_RESET);
        emailDto.setBtnURL(url + passwordResetPath + "?token=" + token);
        rabbitTemplate.convertAndSend("", routingKey, emailDto);
    }

    private EmailDTO baseEmail(User user, EmailType type) {
        EmailDTO emailDto = new EmailDTO();
        emailDto.setEmailTo(user.getEmail());
        emailDto.setUserID(user.getId());
        emailDto.setUsername(user.getUsername());
        emailDto.setLocaleTag("pt-BR");
        emailDto.setEmailType(type);
        return emailDto;
    }
}
