package com.edu.silva.tasks.domain.dtos.responses;

import com.edu.silva.tasks.domain.entities.TaskCompletion;
import lombok.Getter;

import java.time.LocalDate;
import java.util.Date;

@Getter
public class DailyTaskCompletionDTO {
    private final LocalDate completionDate;
    private final Date completedAt;

    public DailyTaskCompletionDTO(TaskCompletion completion) {
        this.completionDate = completion.getCompletionDate();
        this.completedAt = completion.getCompletedAt();
    }
}
