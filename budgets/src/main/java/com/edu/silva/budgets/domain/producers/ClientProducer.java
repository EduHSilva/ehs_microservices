package com.edu.silva.budgets.domain.producers;

import com.edu.silva.common.dtos.EmailDTO;
import com.edu.silva.budgets.domain.entities.Client;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ClientProducer {
    final RabbitTemplate rabbitTemplate;

    public ClientProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Value(value = "${broker.queue.email.name}")
    private String routingKey;

    @Value(value = "${server.url}")
    private String url;

    public void publishMessageEmail(Client client) {
        EmailDTO emailDto = new EmailDTO();
        emailDto.setEmailTo(client.getEmail());
        emailDto.setUserID(client.getId());
        emailDto.setUsername(client.getName());
        emailDto.setLocaleTag("pt-BR");
//        emailDto.setEmailType(EmailType.NEW_USER);

        rabbitTemplate.convertAndSend("", routingKey, emailDto);
    }

}
