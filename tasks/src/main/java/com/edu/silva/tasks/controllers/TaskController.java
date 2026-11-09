package com.edu.silva.tasks.controllers;

import com.edu.silva.common.DefaultResponse;
import com.edu.silva.common.PageResponse;
import com.edu.silva.tasks.domain.dtos.requests.AddTaskDTO;
import com.edu.silva.tasks.domain.dtos.requests.UpdateTaskDTO;
import com.edu.silva.tasks.domain.dtos.responses.TaskResponseDTO;
import com.edu.silva.tasks.services.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.time.LocalDate;

@RestController
@Tag(name = "Tasks", description = "Create, manage and track one-time and daily tasks")
public class TaskController {
    private final TaskService service;

    private final String BASE_URL = "/tasks";

    TaskController(TaskService service) {
        this.service = service;
    }

    @GetMapping()
    @Operation(
            summary = "List tasks in a date interval",
            description = """
                    Returns tasks whose active interval overlaps the effective filter interval:
                    task.startDate <= end AND (task.endDate is empty OR task.endDate >= start).

                    When start is omitted, it defaults to today. When end is omitted, it defaults to seven days after start.
                    For DAILY tasks, dailyCompletions contains only completions whose completionDate falls within the effective interval.
                    dailyStatistics contains the task's accumulated daily statistics.
                    """
    )
    ResponseEntity<@NonNull DefaultResponse<PageResponse<TaskResponseDTO>>> all(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "First date of the filter interval (yyyy-MM-dd). Defaults to today.", example = "2026-10-02")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @Parameter(description = "Last date of the filter interval (yyyy-MM-dd). Defaults to seven days after start.", example = "2026-10-09")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @Parameter(description = "Only tasks linked to this project (id from the study service).")
            @RequestParam(required = false) UUID projectId,
            @Parameter(description = "Only tasks linked to this project stage (id from the study service). Takes precedence over projectId.")
            @RequestParam(required = false) UUID stageId
    ) {
        Page<@NonNull TaskResponseDTO> users = service.findAll(page, size, start, end, projectId, stageId);
        return ResponseEntity.ok(DefaultResponse.paged("Find all tasks successfully", users, BASE_URL));
    }

    @GetMapping("/{id}")
    ResponseEntity<@NonNull DefaultResponse<TaskResponseDTO>> one(@PathVariable UUID id) {
        return ResponseEntity.ok(new DefaultResponse<>("Task find successfully", service.findById(id), BASE_URL, id));
    }

    @PostMapping()
    ResponseEntity<@NonNull DefaultResponse<TaskResponseDTO>> save(@RequestBody @Valid AddTaskDTO dto) {
        TaskResponseDTO t = service.save(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new DefaultResponse<>("Task successfully saved", t, BASE_URL, t.getId())
        );
    }

    @PutMapping("/{id}")
    ResponseEntity<@NonNull DefaultResponse<TaskResponseDTO>> update(@RequestBody UpdateTaskDTO request, @PathVariable UUID id) {
        return ResponseEntity.ok(new DefaultResponse<>("Task successfully updated", service.update(id, request), BASE_URL, id));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<@NonNull DefaultResponse<Void>> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.ok(new DefaultResponse<Void>("Task successfully deleted"));
    }
}
