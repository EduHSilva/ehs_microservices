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
import com.edu.silva.tasks.infra.config.RequestTimeZone;
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
        task.setProjectId(dto.projectId());
        task.setStageId(dto.stageId());
        task.setUserId(UserContext.getUser());
        repository.save(task);
        return toResponse(task, false, null, null);
    }

    @Override
    public void delete(UUID id) {
        repository.delete(findEntity(id));
    }

    @Override
    @Transactional
    public Page<@NonNull TaskResponseDTO> findAll(int page, int size, LocalDate start, LocalDate end, UUID projectId, UUID stageId) {
        Pageable pageable = PageRequest.of(page, size);
        // start/end chegam como dias de calendário do cliente, então não passam por conversão de fuso.
        LocalDate startDay = start == null ? RequestTimeZone.today() : start;
        LocalDate endDay = end == null ? startDay.plusDays(7) : end;
        Date effectiveStart = atStartOfDay(startDay);
        Date effectiveEnd = atEndOfDay(endDay);

        if (effectiveEnd.before(effectiveStart)) {
            return Page.empty(pageable);
        }

        UUID userId = UserContext.getUser();
        Page<Task> tasks;
        if (stageId != null) {
            tasks = repository.findAllOverlappingPeriodByStageId(userId, effectiveStart, effectiveEnd, stageId, pageable);
        } else if (projectId != null) {
            tasks = repository.findAllOverlappingPeriodByProjectId(userId, effectiveStart, effectiveEnd, projectId, pageable);
        } else {
            tasks = repository.findAllOverlappingPeriod(userId, effectiveStart, effectiveEnd, pageable);
        }
        // O atraso é derivado do prazo: toda leitura recalcula e persiste a mudança. Como as
        // entidades estão gerenciadas pela transação, o flush grava o novo status no commit.
        tasks.getContent().forEach(this::refreshDelayedStatus);
        return tasks.map(task -> toResponse(task, true, effectiveStart, effectiveEnd));
    }

    @Override
    @Transactional
    public TaskResponseDTO findById(UUID id) {
        Task task = findEntity(id);
        refreshDelayedStatus(task);
        return toResponse(task, true, null, null);
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
            if (request.type() == TaskType.DAILY) {
                task.setCompletedDate(null);
            }
        } else if (task.getType() == null) {
            task.setType(TaskType.UNIQUE);
        }
        task.setStartDate(request.startDate() == null ? task.getStartDate() : request.startDate());
        if (request.endDate() != null) {
            task.setEndDate(request.endDate());
        }
        if (request.projectId() != null) {
            task.setProjectId(request.projectId());
        }
        if (request.stageId() != null) {
            task.setStageId(request.stageId());
        }

        if (request.status() != null) {
            updateStatus(task, request.status(), request.completionDate());
        }
        // Estender o prazo tira o atraso; mover para PENDING uma tarefa vencida devolve DELAYED.
        refreshDelayedStatus(task);

        return toResponse(repository.save(task), true, null, null);
    }

    private Task findEntity(UUID id) {
        return repository.findByIdAndUserId(id, UserContext.getUser())
                .orElseThrow(() -> new CustomExceptions.EntityNotFoundException(Task.class.getSimpleName(), id));
    }

    private void updateStatus(Task task, TaskStatus status, LocalDate completionDate) {
        LocalDate date = completionDate == null ? RequestTimeZone.today() : completionDate;

        // A single task keeps the day it was executed, so the calendar can show it only there.
        // Reopening it clears the day and it goes back to covering the whole period.
        if (task.getType() != TaskType.DAILY) {
            task.setCompletedDate(status == TaskStatus.DONE ? date : null);
            task.setStatus(status);
            return;
        }

        // A DAILY has no DELAYED: WORKING only means "in progress" and does not depend on a
        // valid completion day, so moving out of DONE just clears today's record.
        if (status == TaskStatus.WORKING) {
            completionRepository.deleteByTaskIdAndCompletionDate(task.getId(), date);
            task.setStatus(TaskStatus.WORKING);
            return;
        }

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
                    "Daily tasks only accept PENDING, WORKING or DONE as a completion status"
            );
        }

        // A daily task is never permanently DONE: its status is derived per day. Keep WORKING so
        // an in-progress recurrence goes back to its lane when today's completion is undone.
        task.setStatus(task.getStatus() == TaskStatus.WORKING ? TaskStatus.WORKING : TaskStatus.PENDING);
    }

    /**
     * O atraso de uma tarefa UNIQUE é derivado do prazo, não escolhido no cliente: uma tarefa
     * vencida e ainda não iniciada vira DELAYED, e estender o prazo a devolve para PENDING.
     * WORKING e DONE são estados explícitos e nunca são sobrescritos; DAILY não tem atraso.
     */
    private void refreshDelayedStatus(Task task) {
        if (task.getType() == TaskType.DAILY) {
            return;
        }
        boolean pastDue = task.getEndDate() != null
                && asLocalDate(task.getEndDate()).isBefore(RequestTimeZone.today());
        if (pastDue && task.getStatus() == TaskStatus.PENDING) {
            task.setStatus(TaskStatus.DELAYED);
        } else if (!pastDue && task.getStatus() == TaskStatus.DELAYED) {
            task.setStatus(TaskStatus.PENDING);
        }
    }

    private TaskResponseDTO toResponse(Task task, boolean includeDailyStatistics, Date filterStart, Date filterEnd) {
        TaskResponseDTO response = new TaskResponseDTO(task);
        // Records created before the recurrence field was added are single tasks.
        if (response.getType() == null) {
            response.setType(TaskType.UNIQUE);
        }
        if (response.getType() == TaskType.DAILY
                && isScheduledOn(task, RequestTimeZone.today())
                && completionRepository.existsByTaskIdAndCompletionDate(task.getId(), RequestTimeZone.today())) {
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
                task.getEndDate() == null ? RequestTimeZone.today() : asLocalDate(task.getEndDate()),
                filterEnd == null ? RequestTimeZone.today() : asLocalDate(filterEnd)
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
        LocalDate today = RequestTimeZone.today();
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
            return RequestTimeZone.today();
        }
        return value.toInstant().atZone(RequestTimeZone.get()).toLocalDate();
    }

    private LocalDate min(LocalDate first, LocalDate second) {
        return first.isBefore(second) ? first : second;
    }

    private LocalDate max(LocalDate first, LocalDate second) {
        return first.isAfter(second) ? first : second;
    }

    private Date atStartOfDay(LocalDate date) {
        return Date.from(date.atStartOfDay(RequestTimeZone.get()).toInstant());
    }

    private Date atEndOfDay(LocalDate date) {
        return Date.from(date.plusDays(1).atStartOfDay(RequestTimeZone.get()).minusNanos(1).toInstant());
    }

    private boolean isScheduledOn(Task task, LocalDate date) {
        LocalDate start = asLocalDate(task.getStartDate() != null ? task.getStartDate() : task.getCreatedDate());
        if (date.isBefore(start)) {
            return false;
        }
        return task.getEndDate() == null || !date.isAfter(asLocalDate(task.getEndDate()));
    }

    private void validateCompletionDate(Task task, LocalDate date) {
        if (date.isAfter(RequestTimeZone.today())) {
            throw new CustomExceptions.InvalidTaskCompletionException("Completion date cannot be in the future");
        }
        if (!isScheduledOn(task, date)) {
            throw new CustomExceptions.InvalidTaskCompletionException(
                    "Completion date must be within the task start and end dates"
            );
        }
    }
}
