package com.silva.edu.finances.domain.entities;

import com.silva.edu.finances.domain.enums.RecurrenceStatus;
import com.silva.edu.finances.domain.enums.RecurrenceType;
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
@Table(name = "recurrences", indexes = @Index(name = "idx_recurrence_user_account", columnList = "user_id, account_id"))
@SQLDelete(sql = "UPDATE recurrences SET status = 'DELETED' WHERE id=?")
@SQLRestriction("status <> 'DELETED'")
@Filter(name = "userFilter", condition = "user_id = :userId")
public class Recurrence implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @CreationTimestamp
    private Date createdDate;
    @UpdateTimestamp
    private Date updatedDate;
    private String title;
    private int day;
    @Column(precision = 19, scale = 4)
    private java.math.BigDecimal value;
    private boolean income;
    @ManyToOne
    private Category category;
    @Enumerated(EnumType.STRING)
    private RecurrenceType type;
    private Integer installments;
    private Integer payed;
    @Enumerated(EnumType.STRING)
    private RecurrenceStatus status;
    @Column(name = "user_id")
    private UUID userId;
    @Column(name = "account_id", length = 80)
    private String accountId;
}
