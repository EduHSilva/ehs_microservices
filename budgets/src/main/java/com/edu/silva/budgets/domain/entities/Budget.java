package com.edu.silva.budgets.domain.entities;

import com.edu.silva.budgets.domain.enums.BudgetStatus;
import jakarta.persistence.*;
import jakarta.persistence.CascadeType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "budgets")
@SQLDelete(sql = "UPDATE budgets SET status = 'DELETED' WHERE id=?")
@SQLRestriction("status <> 'DELETED'")
@FilterDef(
        name = "userFilter",
        parameters = @ParamDef(name = "userId", type = UUID.class)
)
@Filter(
        name = "userFilter",
        condition = "user_id = :userId"
)
@ToString(exclude = {"items", "client"})
public class Budget implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @CreationTimestamp
    private Date createdDate;
    @UpdateTimestamp
    private Date updatedDate;
    private String title;
    @Column(length = 1000)
    private String description;
    @Column(length = 1000)
    private String terms;
    @Column(length = 1000)
    private String observations;
    private int validate;
    @Enumerated(EnumType.STRING)
    private BudgetStatus status;
    @ManyToOne
    private Client client;
    @Column(name = "user_id")
    private UUID userId;
    @Column(name = "kanban_order")
    private Integer kanbanOrder;

    @OneToMany(mappedBy = "budget", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Item> items = new ArrayList<>();
}
