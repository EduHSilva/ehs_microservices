package com.silva.edu.finances.domain.entities;

import com.silva.edu.finances.domain.enums.CardStatus;
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
import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "credit_card", indexes = @Index(name = "idx_credit_card_account", columnList = "account_id"))
@SQLDelete(sql = "UPDATE credit_card SET status = 'DELETED' WHERE id=?")
@SQLRestriction("status <> 'DELETED'")
@FilterDef(name = "userFilter", parameters = @ParamDef(name = "userId", type = UUID.class))
@Filter(name = "userFilter", condition = "user_id = :userId or account_id in (select m.account_id from finance_account_member m where m.user_id = :userId and m.status = 'ACTIVE')")
public class CreditCard implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @CreationTimestamp
    private Date createdDate;
    @UpdateTimestamp
    private Date updatedDate;
    @Column(name = "user_id")
    private UUID userId;
    @Column(name = "account_id", length = 80)
    private String accountId;
    @Column(length = 120)
    private String name;
    @Column(name = "card_limit", precision = 19, scale = 4)
    private BigDecimal cardLimit;
    @Column(name = "closing_day")
    private int closingDay;
    @Column(name = "due_day")
    private int dueDay;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20) default 'ACTIVE'")
    private CardStatus status;
}
