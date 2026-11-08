package com.edu.silva.budgets.domain.entities;

import com.edu.silva.common.enums.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.*;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "item")
@SQLDelete(sql = "UPDATE item SET status = 'DELETED' WHERE id=?")
@SQLRestriction("status <> 'DELETED'")
@ToString(exclude = "budget")
public class Item implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @CreationTimestamp
    private Date createdDate;
    @UpdateTimestamp
    private Date updatedDate;
    private String name;
    @Column(length = 1000)
    private String description;
    private Integer quantity;
    private Double priceUn;
    @Enumerated(EnumType.STRING)
    private Status status;
    @Column(name = "user_id")
    private UUID userId;
    @ManyToOne
    @JoinColumn(name = "budget_id")
    private Budget budget;
}
