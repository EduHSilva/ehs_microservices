package com.edu.silva.tasks.domain.dtos.requests;

import com.edu.silva.tasks.domain.enums.TaskStatus;
import com.edu.silva.tasks.domain.enums.TaskType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/** All fields are optional so a daily completion can be updated independently. */
public record UpdateTaskDTO(
        String title,
        TaskType type,
        Date startDate,
        Date endDate,
        TaskStatus status,
        LocalDate completionDate,
        UUID projectId,
        UUID stageId,
        /** ISO weekdays (1 = Monday .. 7 = Sunday) used by WEEKLY. */
        List<@Min(1) @Max(7) Integer> recurrenceDays,
        /** Day of month (1..31) used by MONTHLY. */
        @Min(1) @Max(31) Integer recurrenceDayOfMonth,
        /** Completions per week used by WEEKLY_GOAL. */
        @Min(1) @Max(7) Integer weeklyGoal
) {
}
