package com.edu.silva.users.repositories;

import com.edu.silva.users.domain.entities.User;
import com.edu.silva.users.domain.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Date;
import java.util.List;
import java.util.UUID;


public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
    UserDetails findByEmail(String email);

    long countByStatus(UserStatus status);

    long countByCompanyIsNull();

    @Query("select u.createdDate from User u")
    List<Date> findAllCreatedDates();
}