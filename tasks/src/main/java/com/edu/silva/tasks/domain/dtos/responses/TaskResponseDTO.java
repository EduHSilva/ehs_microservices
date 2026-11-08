package com.edu.silva.tasks.domain.dtos.responses;

import com.edu.silva.tasks.domain.entities.Task;
import com.edu.silva.tasks.domain.enums.TaskStatus;
import com.edu.silva.tasks.domain.enums.TaskType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.BeanUtils;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class TaskResponseDTO {
    private UUID id;
    private Date createdDate;
    private String title;
    private Date startDate;
    private Date endDate;
    private TaskType type;
    private TaskStatus status;
    @Schema(description = "Day a single task was completed. Null while pending and for DAILY tasks.")
    private LocalDate completedDate;
    @Schema(description = "Project this task belongs to (id from the study service).")
    private UUID projectId;
    @Schema(description = "Project stage this task belongs to (id from the study service).")
    private UUID stageId;
    @Schema(description = "Accumulated completion statistics. Present for DAILY tasks.")
    private DailyTaskStatisticsDTO dailyStatistics;
    @Schema(description = "Daily completion records. In the task list, only records inside the effective start/end interval are returned; in task detail, the full history is returned.")
    private List<DailyTaskCompletionDTO> dailyCompletions;

    public TaskResponseDTO(Task task) {
        BeanUtils.copyProperties(task, this);
    }
}
