package com.silva.edu.finances.domain.entities;

import com.silva.edu.finances.domain.enums.GoalMode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "monthly_goal", indexes = @Index(name = "idx_goal_user_account_period", columnList = "user_id, account_id, period_month"))
@Getter @Setter @NoArgsConstructor
public class MonthlyGoal {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;
    @Column(name = "account_id", nullable = false, length = 80)
    private String accountId;
    @Column(nullable = false, length = 120)
    private String title;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24)
    private GoalMode mode;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;
    @Column(name = "initial_balance", precision = 19, scale = 4)
    private BigDecimal initialBalance = BigDecimal.ZERO;
    @Column(precision = 19, scale = 4)
    private BigDecimal objective;
    @Column(name = "period_month", nullable = false, length = 7)
    private String periodMonth;
    @Column(name = "dt_inicio")
    private LocalDate dtInicio;
    @Column(name = "dt_fim")
    private LocalDate dtFim;
    @Column(nullable = false)
    private boolean active = true;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
