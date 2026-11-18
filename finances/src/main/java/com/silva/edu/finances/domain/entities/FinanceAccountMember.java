package com.silva.edu.finances.domain.entities;

import com.silva.edu.finances.domain.enums.AccountMemberStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "finance_account_member",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_account_member_account_email", columnNames = {"account_id", "email"}),
                @UniqueConstraint(name = "uk_account_member_account_user", columnNames = {"account_id", "user_id"}),
                @UniqueConstraint(name = "uk_account_member_token", columnNames = {"token"})
        },
        indexes = @Index(name = "idx_account_member_user", columnList = "user_id, status"))
@Getter @Setter @NoArgsConstructor
public class FinanceAccountMember {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "account_id", nullable = false, length = 80)
    private String accountId;
    @Column(nullable = false, length = 255)
    private String email;
    @Column(name = "user_id")
    private UUID userId;
    @Column(length = 255)
    private String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private AccountMemberStatus status;
    @Column(length = 64)
    private String token;
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;
}
