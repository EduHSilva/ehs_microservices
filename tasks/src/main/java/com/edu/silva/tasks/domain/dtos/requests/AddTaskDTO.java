package com.edu.silva.tasks.domain.dtos.requests;

import com.edu.silva.tasks.domain.enums.TaskStatus;
import com.edu.silva.tasks.domain.enums.TaskType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public record AddTaskDTO(
        @NotBlank String title,
        TaskType type,
        Date startDate,
        Date endDate,
        TaskStatus status,
        UUID projectId,
        UUID stageId,
        /** ISO weekdays (1 = Monday .. 7 = Sunday); required for WEEKLY. */
        List<@Min(1) @Max(7) Integer> recurrenceDays,
        /** Day of month (1..31); required for MONTHLY. */
        @Min(1) @Max(31) Integer recurrenceDayOfMonth,
        /** Completions per week; required for WEEKLY_GOAL. */
        @Min(1) @Max(7) Integer weeklyGoal
) {
}
