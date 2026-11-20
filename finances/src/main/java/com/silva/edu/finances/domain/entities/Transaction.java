package com.silva.edu.finances.domain.entities;

import com.silva.edu.finances.domain.enums.TransactionEntryType;
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
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "transaction",
        uniqueConstraints = @UniqueConstraint(name = "uk_transaction_import", columnNames = {"user_id", "account_id", "import_key"}),
        indexes = {
                @Index(name = "idx_transaction_user_account_date", columnList = "user_id, account_id, execution_date"),
                @Index(name = "idx_transaction_account_card_date", columnList = "account_id, credit_card_id, execution_date"),
                @Index(name = "idx_transaction_account_transfer", columnList = "account_id, transfer_id")
        })
@SQLDelete(sql = "UPDATE transaction SET status = 'DELETED' WHERE id=?")
@SQLRestriction("status <> 'DELETED'")
@FilterDef(name = "userFilter", parameters = @ParamDef(name = "userId", type = UUID.class))
@Filter(name = "userFilter", condition = "user_id = :userId or account_id in (select m.account_id from finance_account_member m where m.user_id = :userId and m.status = 'ACTIVE')")
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
    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, columnDefinition = "varchar(20) default 'NORMAL'")
    private TransactionEntryType entryType;
    @ManyToOne
    private CreditCard creditCard;
    @Column(name = "transfer_id")
    private UUID transferId;
    @Column(name = "invoice_due_date")
    private Date invoiceDueDate;
}
