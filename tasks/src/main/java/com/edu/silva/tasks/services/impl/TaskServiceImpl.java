package com.edu.silva.tasks.services.impl;

import com.edu.silva.tasks.domain.dtos.requests.AddTaskDTO;
import com.edu.silva.tasks.domain.dtos.requests.UpdateTaskDTO;
import com.edu.silva.tasks.domain.dtos.responses.DailyTaskStatisticsDTO;
import com.edu.silva.tasks.domain.dtos.responses.DailyTaskCompletionDTO;
import com.edu.silva.tasks.domain.dtos.responses.TaskResponseDTO;
import com.edu.silva.tasks.domain.entities.Task;
import com.edu.silva.tasks.domain.entities.TaskCompletion;
import com.edu.silva.tasks.domain.enums.TaskStatus;
import com.edu.silva.tasks.domain.enums.TaskType;
import com.edu.silva.tasks.infra.config.UserContext;
import com.edu.silva.tasks.infra.exceptions.CustomExceptions;
import com.edu.silva.tasks.repositories.TaskRepository;
import com.edu.silva.tasks.repositories.TaskCompletionRepository;
import com.edu.silva.tasks.services.TaskService;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.Date;

@Service
public class TaskServiceImpl implements TaskService {

    private final TaskRepository repository;
    private final TaskCompletionRepository completionRepository;

    public TaskServiceImpl(TaskRepository repository, TaskCompletionRepository completionRepository) {
        this.repository = repository;
        this.completionRepository = completionRepository;
    }

    @Transactional
    public TaskResponseDTO save(AddTaskDTO dto) {
        Task task = new Task();
        task.setTitle(dto.title());
        task.setType(dto.type() == null ? TaskType.UNIQUE : dto.type());
        task.setStartDate(dto.startDate() == null ? new java.util.Date() : dto.startDate());
        task.setEndDate(dto.endDate());
        task.setStatus(TaskStatus.PENDING);
        task.setUserId(UserContext.getUser());
        repository.save(task);
        return toResponse(task, false, null, null);
    }

    @Override
    public void delete(UUID id) {
        repository.deleteById(id);
    }

    @Override
    public Page<@NonNull TaskResponseDTO> findAll(int page, int size, Date start, Date end) {
        Pageable pageable = PageRequest.of(page, size);
        Date effectiveStart = atStartOfDay(start == null ? LocalDate.now() : asLocalDate(start));
        Date effectiveEnd = atEndOfDay(end == null
                ? asLocalDate(effectiveStart).plusDays(7)
                : asLocalDate(end));

        if (effectiveEnd.before(effectiveStart)) {
            return Page.empty(pageable);
        }

        Page<Task> tasks = repository.findAllOverlappingPeriod(effectiveStart, effectiveEnd, pageable);
        return tasks.map(task -> toResponse(task, true, effectiveStart, effectiveEnd));
    }

    @Override
    public TaskResponseDTO findById(UUID id) {
        return toResponse(findEntity(id), true, null, null);
    }

    @Override
    @Transactional
    public TaskResponseDTO update(UUID id, UpdateTaskDTO request) {
        Task task = findEntity(id);
        if (request.title() != null) {
            task.setTitle(request.title());
        }
        if (request.type() != null) {
            task.setType(request.type());
        } else if (task.getType() == null) {
            task.setType(TaskType.UNIQUE);
        }
        task.setStartDate(request.startDate() == null ? task.getStartDate() : request.startDate());
        if (request.endDate() != null) {
            task.setEndDate(request.endDate());
        }

        if (request.status() != null) {
            updateStatus(task, request.status(), request.completionDate());
        }

        return toResponse(repository.save(task), true, null, null);
    }

    private Task findEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new CustomExceptions.EntityNotFoundException(Task.class.getSimpleName(), id));
    }

    private void updateStatus(Task task, TaskStatus status, LocalDate completionDate) {
        if (task.getType() != TaskType.DAILY) {
            task.setStatus(status);
            return;
        }

        LocalDate date = completionDate == null ? LocalDate.now() : completionDate;
        validateCompletionDate(task, date);
        if (status == TaskStatus.DONE) {
            if (!completionRepository.existsByTaskIdAndCompletionDate(task.getId(), date)) {
                TaskCompletion completion = new TaskCompletion();
                completion.setTask(task);
                completion.setCompletionDate(date);
                completionRepository.save(completion);
            }
        } else if (status == TaskStatus.PENDING) {
            completionRepository.deleteByTaskIdAndCompletionDate(task.getId(), date);
        } else {
            throw new CustomExceptions.InvalidTaskCompletionException(
                    "Daily tasks only accept DONE or PENDING as a completion status"
            );
        }

        // A daily task is never permanently DONE: its status is derived per day.
        task.setStatus(TaskStatus.PENDING);
    }

    private TaskResponseDTO toResponse(Task task, boolean includeDailyStatistics, Date filterStart, Date filterEnd) {
        TaskResponseDTO response = new TaskResponseDTO(task);
        // Records created before the recurrence field was added are single tasks.
        if (response.getType() == null) {
            response.setType(TaskType.UNIQUE);
        }
        if (response.getType() == TaskType.DAILY
                && isScheduledOn(task, LocalDate.now())
                && completionRepository.existsByTaskIdAndCompletionDate(task.getId(), LocalDate.now())) {
            response.setStatus(TaskStatus.DONE);
        }
        if (includeDailyStatistics && response.getType() == TaskType.DAILY) {
            response.setDailyStatistics(dailyStatistics(task));
        }
        if (response.getType() == TaskType.DAILY) {
            response.setDailyCompletions(findCompletionsInPeriod(task, filterStart, filterEnd));
        }
        return response;
    }

    private java.util.List<DailyTaskCompletionDTO> findCompletionsInPeriod(Task task, Date filterStart, Date filterEnd) {
        LocalDate start = max(
                asLocalDate(task.getStartDate() != null ? task.getStartDate() : task.getCreatedDate()),
                filterStart == null ? LocalDate.MIN : asLocalDate(filterStart)
        );
        LocalDate end = min(
                task.getEndDate() == null ? LocalDate.now() : asLocalDate(task.getEndDate()),
                filterEnd == null ? LocalDate.now() : asLocalDate(filterEnd)
        );
        if (start.isAfter(end)) {
            return java.util.List.of();
        }
        return completionRepository.findByTaskIdAndCompletionDateBetweenOrderByCompletionDateAsc(task.getId(), start, end)
                .stream()
                .map(DailyTaskCompletionDTO::new)
                .toList();
    }

    private DailyTaskStatisticsDTO dailyStatistics(Task task) {
        LocalDate start = asLocalDate(task.getStartDate() != null ? task.getStartDate() : task.getCreatedDate());
        LocalDate today = LocalDate.now();
        LocalDate lastScheduledDay = task.getEndDate() == null
                ? today
                : min(asLocalDate(task.getEndDate()), today);

        if (start.isAfter(lastScheduledDay)) {
            return new DailyTaskStatisticsDTO(0, 0, 0, false);
        }

        long completed = completionRepository.countByTaskIdAndCompletionDateBetween(task.getId(), start, lastScheduledDay);
        boolean completedToday = !today.isBefore(start)
                && !today.isAfter(lastScheduledDay)
                && completionRepository.existsByTaskIdAndCompletionDate(task.getId(), today);

        // Today only becomes a skipped day after it has elapsed.
        LocalDate lastPastDay = min(lastScheduledDay, today.minusDays(1));
        long elapsedDays = start.isAfter(lastPastDay) ? 0 : start.datesUntil(lastPastDay.plusDays(1)).count();
        long completedPastDays = elapsedDays == 0 ? 0
                : completionRepository.countByTaskIdAndCompletionDateBetween(task.getId(), start, lastPastDay);
        long skipped = elapsedDays - completedPastDays;
        long scheduled = start.datesUntil(lastScheduledDay.plusDays(1)).count();

        return new DailyTaskStatisticsDTO(scheduled, completed, skipped, completedToday);
    }

    private LocalDate asLocalDate(java.util.Date value) {
        if (value == null) {
            return LocalDate.now();
        }
        return value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private LocalDate min(LocalDate first, LocalDate second) {
        return first.isBefore(second) ? first : second;
    }

    private LocalDate max(LocalDate first, LocalDate second) {
        return first.isAfter(second) ? first : second;
    }

    private Date atStartOfDay(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private Date atEndOfDay(LocalDate date) {
        return Date.from(date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).minusNanos(1).toInstant());
    }

    private boolean isScheduledOn(Task task, LocalDate date) {
        LocalDate start = asLocalDate(task.getStartDate() != null ? task.getStartDate() : task.getCreatedDate());
        if (date.isBefore(start)) {
            return false;
        }
        return task.getEndDate() == null || !date.isAfter(asLocalDate(task.getEndDate()));
    }

    private void validateCompletionDate(Task task, LocalDate date) {
        if (date.isAfter(LocalDate.now())) {
            throw new CustomExceptions.InvalidTaskCompletionException("Completion date cannot be in the future");
        }
        if (!isScheduledOn(task, date)) {
            throw new CustomExceptions.InvalidTaskCompletionException(
                    "Completion date must be within the task start and end dates"
            );
        }
    }
}
