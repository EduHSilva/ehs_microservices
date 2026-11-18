package com.silva.edu.finances.domain.entities;

import com.silva.edu.finances.domain.catalog.DefaultCategories.DefaultCategory;
import com.silva.edu.finances.domain.enums.CategoryClassification;
import com.silva.edu.finances.domain.enums.CategoryStatus;
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
@Table(name = "category")
@SQLDelete(sql = "UPDATE category SET status = 'DELETED' WHERE id=?")
@SQLRestriction("status <> 'DELETED'")
@FilterDef(name = "userFilter", parameters = @ParamDef(name = "userId", type = UUID.class))
@Filter(name = "userFilter", condition = "user_id = :userId"
        + " or user_id in (select a.owner_id from finance_account a join finance_account_member m on m.account_id = a.id"
        + " where m.user_id = :userId and m.status = 'ACTIVE')"
        + " or user_id in (select m.user_id from finance_account_member m join finance_account a on a.id = m.account_id"
        + " where a.owner_id = :userId and m.status = 'ACTIVE' and m.user_id is not null)")
public class Category implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @CreationTimestamp
    private Date createdDate;
    @UpdateTimestamp
    private Date updatedDate;
    private String title;
    @Enumerated(EnumType.STRING)
    private CategoryStatus status;
    private String color;
    private int goal;
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean income;
    @Enumerated(EnumType.STRING)
    private CategoryClassification classification;
    @Column(name = "default_key")
    private String defaultKey;
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean customized;
    @Column(name = "user_id")
    private UUID userId;

    public static Category fromDefault(DefaultCategory entry, UUID userId) {
        Category category = new Category();
        category.setTitle(entry.title());
        category.setColor(entry.color());
        category.setIncome(entry.income());
        category.setGoal(0);
        category.setClassification(entry.classification());
        category.setStatus(CategoryStatus.ACTIVE);
        category.setDefaultKey(entry.key());
        category.setUserId(userId);
        return category;
    }
}
