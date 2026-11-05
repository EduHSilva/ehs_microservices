package com.silva.edu.finances.domain.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "goal_contribution", indexes = @Index(name = "idx_contribution_goal_date", columnList = "goal_id, contribution_date"))
@Getter @Setter @NoArgsConstructor
public class GoalContribution {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goal_id", nullable = false)
    private MonthlyGoal goal;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;
    @Column(name = "contribution_date", nullable = false)
    private LocalDate date;
    @Column(name = "transaction_id", unique = true)
    private UUID transactionId;
}
