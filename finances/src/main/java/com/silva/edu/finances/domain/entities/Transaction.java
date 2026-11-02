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
@Table(name = "transaction")
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
}
