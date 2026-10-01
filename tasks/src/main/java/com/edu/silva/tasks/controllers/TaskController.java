package com.edu.silva.tasks.controllers;

import com.edu.silva.common.DefaultResponse;
import com.edu.silva.common.PageResponse;
import com.edu.silva.tasks.domain.dtos.requests.AddTaskDTO;
import com.edu.silva.tasks.domain.dtos.responses.TaskResponseDTO;
import com.edu.silva.tasks.services.TaskService;
import jakarta.validation.Valid;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class TaskController {
    private final TaskService service;

    private final String BASE_URL = "/tasks";

    TaskController(TaskService service) {
        this.service = service;
    }

    @GetMapping()
    ResponseEntity<@NonNull DefaultResponse<PageResponse<TaskResponseDTO>>> all(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<@NonNull TaskResponseDTO> users = service.findAll(page, size);
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
    ResponseEntity<@NonNull DefaultResponse<TaskResponseDTO>> update(@RequestBody @Valid AddTaskDTO request, @PathVariable UUID id) {
        return ResponseEntity.ok(new DefaultResponse<>("Task successfully updated", service.update(id, request), BASE_URL, id));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<@NonNull DefaultResponse<Void>> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.ok(new DefaultResponse<Void>("Task successfully deleted"));
    }
}
