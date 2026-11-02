package com.edu.silva.users.domain.dtos.responses;

import com.edu.silva.users.domain.entities.Company;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class CompanyResponseDTO {
    private UUID id;
    private Date createdDate;
    private String name;
    private List<UserResponseDTO> users = new ArrayList<>();

    public CompanyResponseDTO(Company company) {
        BeanUtils.copyProperties(company, this);
        if (company.getUsers() != null) {
            this.users = company.getUsers().stream().map(UserResponseDTO::new).toList();
        } else {
            this.users = new ArrayList<>();
        }
    }
}
