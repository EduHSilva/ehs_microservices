package com.edu.silva.budgets.domain.dtos.responses;

import com.edu.silva.budgets.domain.entities.Client;
import com.edu.silva.budgets.domain.enums.ClientStatus;
import lombok.*;
import org.springframework.beans.BeanUtils;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClientResponseDTO {
    private UUID id;
    private String name;
    private String email;
    private String phone;
    private ClientStatus status;
    private String observations;

    public ClientResponseDTO(Client client) {
        BeanUtils.copyProperties(client, this);
    }
}
