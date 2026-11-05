package com.silva.edu.finances.domain.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_preference")
public class UserPreference {
    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "show_business_account", nullable = false)
    private boolean showBusinessAccount = true;

    @Column(nullable = false, length = 3)
    private String currency = "BRL";
}
