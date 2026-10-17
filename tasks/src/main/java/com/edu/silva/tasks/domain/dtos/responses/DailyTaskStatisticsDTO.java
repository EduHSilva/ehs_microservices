package com.edu.silva.tasks.domain.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DailyTaskStatisticsDTO {
    private long scheduledDays;
    private long completedDays;
    private long skippedDays;
    private boolean completedToday;
}
