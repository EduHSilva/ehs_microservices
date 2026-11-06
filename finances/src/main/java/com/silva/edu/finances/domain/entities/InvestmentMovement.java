package com.silva.edu.finances.domain.entities;

import com.silva.edu.finances.domain.enums.InvestmentMovementType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "investment_movement", indexes = @Index(name = "idx_investment_movement_user_account_date", columnList = "user_id, account_id, movement_date"))
@Getter @Setter @NoArgsConstructor
public class InvestmentMovement {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;
    @Column(name = "account_id", nullable = false, length = 80)
    private String accountId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private InvestmentMovementType type;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;
    @Column(name = "movement_date", nullable = false)
    private LocalDate date;
    @Column(length = 160)
    private String description;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
