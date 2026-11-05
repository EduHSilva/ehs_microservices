package com.silva.edu.finances.domain.entities;

import com.silva.edu.finances.domain.enums.TransactionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Filter;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "transaction",
        uniqueConstraints = @UniqueConstraint(name = "uk_transaction_import", columnNames = {"user_id", "account_id", "import_key"}),
        indexes = @Index(name = "idx_transaction_user_account_date", columnList = "user_id, account_id, execution_date"))
@SQLDelete(sql = "UPDATE transaction SET status = 'DELETED' WHERE id=?")
@SQLRestriction("status <> 'DELETED'")
@Filter(name = "userFilter", condition = "user_id = :userId")
public class Transaction implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @CreationTimestamp
    private Date createdDate;
    @UpdateTimestamp
    private Date updatedDate;
    private Date executionDate;
    private String title;
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean income;
    @Column(precision = 19, scale = 4)
    private java.math.BigDecimal value;
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;
    @ManyToOne
    private Category category;
    @ManyToOne
    private Recurrence recurrence;
    @Column(name = "user_id")
    private UUID userId;
    @Column(name = "account_id", length = 80)
    private String accountId;
    @Column(name = "import_key", length = 64)
    private String importKey;
    @Column(name = "import_source", length = 40)
    private String importSource;
}
