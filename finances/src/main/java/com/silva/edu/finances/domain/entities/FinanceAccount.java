package com.silva.edu.finances.domain.entities;

import com.silva.edu.finances.domain.enums.FinancialAccountType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.UUID;

@Entity @Table(name = "finance_account", indexes = @Index(name = "idx_finance_account_owner", columnList = "owner_id"))
@Getter @Setter @NoArgsConstructor
public class FinanceAccount {
    @Id @Column(length = 80)
    private String id;
    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;
    @Column(nullable = false, length = 120)
    private String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 2)
    private FinancialAccountType type;
    @Column(name = "opening_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal openingBalance = BigDecimal.ZERO;
    @Column(nullable = false, length = 3)
    private String currency = "BRL";
    @Column(name = "legacy_data_migrated", nullable = false)
    private boolean legacyDataMigrated;
}
