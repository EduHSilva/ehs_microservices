package com.edu.silva.tasks.services;

import com.edu.silva.tasks.domain.dtos.requests.AddTaskDTO;
import com.edu.silva.tasks.domain.dtos.requests.UpdateTaskDTO;
import com.edu.silva.tasks.domain.dtos.responses.TaskResponseDTO;
import lombok.NonNull;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.UUID;

public interface TaskService {
    TaskResponseDTO save(AddTaskDTO request);

    void delete(UUID id);

    Page<@NonNull TaskResponseDTO> findAll(int page, int size, LocalDate start, LocalDate end, UUID projectId, UUID stageId);

    TaskResponseDTO findById(UUID id);

    TaskResponseDTO update(UUID id, UpdateTaskDTO request);

}
