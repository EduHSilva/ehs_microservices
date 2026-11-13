package com.edu.silva.users.repositories;

import com.edu.silva.users.domain.entities.User;
import com.edu.silva.users.domain.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
    @Query("select u from User u where lower(u.email) = lower(:email)")
    UserDetails findByEmail(@Param("email") String email);

    /**
     * Busca ignorando o {@code @SQLRestriction} de soft delete, para detectar
     * e-mails já usados (inclusive por usuários DELETED) sem criar duplicatas.
     */
    @Query(value = "select * from users where lower(email) = lower(:email) limit 1", nativeQuery = true)
    Optional<User> findAnyByEmail(@Param("email") String email);

    long countByStatus(UserStatus status);

    long countByCompanyIsNull();

    @Query("select u.createdDate from User u")
    List<Date> findAllCreatedDates();
}
