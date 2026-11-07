package com.edu.silva.users.domain.dtos.responses;

import com.edu.silva.users.domain.entities.User;
import com.edu.silva.users.domain.enums.UserRole;
import com.edu.silva.users.domain.enums.UserStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.BeanUtils;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class UserResponseDTO {
    private UUID id;
    private Date createdDate;
    private String email;
    private String username;
    private UserStatus status;
    private List<UserRole> roles;
    private String companyName;

    public UserResponseDTO(User user) {
        BeanUtils.copyProperties(user, this);

        this.companyName = user.getCompany() == null ? null : user.getCompany().getName();
    }
}
