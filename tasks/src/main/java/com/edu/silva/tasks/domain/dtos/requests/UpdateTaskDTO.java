package com.edu.silva.tasks.domain.dtos.requests;

import com.edu.silva.tasks.domain.enums.TaskStatus;
import com.edu.silva.tasks.domain.enums.TaskType;

import java.time.LocalDate;
import java.util.Date;
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
        UUID stageId
) {
}
