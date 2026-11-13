package com.edu.silva.tasks.domain.entities;

import com.edu.silva.tasks.domain.enums.TaskStatus;
import com.edu.silva.tasks.domain.enums.TaskType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.*;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tasks")
@SQLDelete(sql = "UPDATE tasks SET status = 'DELETED' WHERE id=?")
@SQLRestriction("status <> 'DELETED'")
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

    /** Day a single task was completed; null while pending and for recurring tasks. */
    @Column(name = "completed_date")
    private LocalDate completedDate;

    /** ISO weekdays (1 = Monday .. 7 = Sunday) used by WEEKLY tasks. */
    @Convert(converter = WeekdayListConverter.class)
    @Column(name = "recurrence_days", length = 32)
    private List<Integer> recurrenceDays;

    /** Day of month (1..31) used by MONTHLY tasks. */
    @Column(name = "recurrence_day_of_month")
    private Integer recurrenceDayOfMonth;

    /** Completions expected per week for WEEKLY_GOAL tasks. */
    @Column(name = "weekly_goal")
    private Integer weeklyGoal;

    /** Optional project this task belongs to (id from the study service). */
    @Column(name = "project_id")
    private UUID projectId;

    /** Optional project stage this task belongs to (id from the study service). */
    @Column(name = "stage_id")
    private UUID stageId;

    @Column(name = "user_id")
    private UUID userId;
}
