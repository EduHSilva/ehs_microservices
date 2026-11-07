package com.edu.silva.tasks.domain.dtos.requests;

import com.edu.silva.tasks.domain.enums.TaskStatus;
import com.edu.silva.tasks.domain.enums.TaskType;
import jakarta.validation.constraints.NotBlank;

import java.util.Date;
import java.util.UUID;

public record AddTaskDTO(
        @NotBlank String title,
        TaskType type,
        Date startDate,
        Date endDate,
        TaskStatus status,
        UUID projectId,
        UUID stageId
) {
}
