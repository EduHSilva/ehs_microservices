package com.edu.silva.tasks.domain.entities;

import com.edu.silva.tasks.domain.enums.TaskStatus;
import com.edu.silva.tasks.domain.enums.TaskType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.*;

import java.util.Date;
import java.util.UUID;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tasks")
@SQLDelete(sql = "UPDATE tasks SET status = 'DELETED' WHERE id=?")
@SQLRestriction("status <> 'DELETED'")
@Filter(
        name = "userFilter",
        condition = "user_id = :userId"
)
@FilterDef(
        name = "userFilter",
        parameters = @ParamDef(name = "userId", type = UUID.class)
)
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @CreationTimestamp
    private Date createdDate;
    @UpdateTimestamp
    private Date updatedDate;
    private String title;
    /** First day on which this task is available. */
    @Column(name = "start_date")
    private Date startDate;
    /** Optional last day on which this task can be completed. */
    @Column(name = "due_date")
    private Date endDate;
    @Enumerated(EnumType.STRING)
    private TaskType type;
    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    @Column(name = "user_id")
    private UUID userId;
}
