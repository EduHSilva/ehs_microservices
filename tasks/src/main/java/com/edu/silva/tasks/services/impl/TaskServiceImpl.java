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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
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
        task.setRecurrenceDays(dto.recurrenceDays());
        task.setRecurrenceDayOfMonth(dto.recurrenceDayOfMonth());
        task.setWeeklyGoal(dto.weeklyGoal());
        normalizeRecurrence(task);
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
            if (request.type().isRecurring()) {
                task.setCompletedDate(null);
                if (task.getStatus() == TaskStatus.DONE || task.getStatus() == TaskStatus.DELAYED) {
                    task.setStatus(TaskStatus.PENDING);
                }
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
        if (request.recurrenceDays() != null) {
            task.setRecurrenceDays(request.recurrenceDays());
        }
        if (request.recurrenceDayOfMonth() != null) {
            task.setRecurrenceDayOfMonth(request.recurrenceDayOfMonth());
        }
        if (request.weeklyGoal() != null) {
            task.setWeeklyGoal(request.weeklyGoal());
        }
        normalizeRecurrence(task);

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
        if (task.getType() == null || !task.getType().isRecurring()) {
            task.setCompletedDate(status == TaskStatus.DONE ? date : null);
            task.setStatus(status);
            return;
        }

        // A recurring task has no DELAYED: WORKING only means "in progress" and does not depend on
        // a valid completion day, so moving out of DONE just clears that day's record.
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
                    "Recurring tasks only accept PENDING, WORKING or DONE as a completion status"
            );
        }

        // A recurring task is never permanently DONE: its status is derived per day. Keep WORKING so
        // an in-progress recurrence goes back to its lane when today's completion is undone.
        task.setStatus(task.getStatus() == TaskStatus.WORKING ? TaskStatus.WORKING : TaskStatus.PENDING);
    }

    /**
     * O atraso de uma tarefa UNIQUE é derivado do prazo, não escolhido no cliente: uma tarefa
     * vencida e ainda não iniciada vira DELAYED, e estender o prazo a devolve para PENDING.
     * WORKING e DONE são estados explícitos e nunca são sobrescritos; recorrentes não têm atraso.
     */
    private void refreshDelayedStatus(Task task) {
        if (task.getType() != null && task.getType().isRecurring()) {
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
        if (response.getType().isRecurring()
                && isScheduledOn(task, RequestTimeZone.today())
                && completionRepository.existsByTaskIdAndCompletionDate(task.getId(), RequestTimeZone.today())) {
            response.setStatus(TaskStatus.DONE);
        }
        if (includeDailyStatistics && response.getType().isRecurring()) {
            response.setDailyStatistics(dailyStatistics(task));
        }
        if (response.getType().isRecurring()) {
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
        if (task.getType() == TaskType.WEEKLY_GOAL) {
            return weeklyGoalStatistics(task);
        }
        LocalDate start = taskStart(task);
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
                && isScheduledOn(task, today)
                && completionRepository.existsByTaskIdAndCompletionDate(task.getId(), today);

        // Today only becomes a skipped day after it has elapsed.
        LocalDate lastPastDay = min(lastScheduledDay, today.minusDays(1));
        long scheduledPast = countScheduledDays(task, start, lastPastDay);
        long completedPast = start.isAfter(lastPastDay) ? 0
                : completionRepository.countByTaskIdAndCompletionDateBetween(task.getId(), start, lastPastDay);
        long skipped = Math.max(0, scheduledPast - completedPast);
        long scheduled = countScheduledDays(task, start, lastScheduledDay);

        return new DailyTaskStatisticsDTO(scheduled, completed, skipped, completedToday);
    }

    /** For WEEKLY_GOAL a scheduled "day" is a weekly slot: the week contributes up to its goal. */
    private DailyTaskStatisticsDTO weeklyGoalStatistics(Task task) {
        LocalDate start = taskStart(task);
        LocalDate today = RequestTimeZone.today();
        LocalDate lastScheduledDay = task.getEndDate() == null
                ? today
                : min(asLocalDate(task.getEndDate()), today);

        if (start.isAfter(lastScheduledDay)) {
            return new DailyTaskStatisticsDTO(0, 0, 0, false);
        }

        int goal = task.getWeeklyGoal() == null ? 1 : task.getWeeklyGoal();
        long completed = completionRepository.countByTaskIdAndCompletionDateBetween(task.getId(), start, lastScheduledDay);
        boolean completedToday = !today.isBefore(start)
                && !today.isAfter(lastScheduledDay)
                && completionRepository.existsByTaskIdAndCompletionDate(task.getId(), today);

        LocalDate currentWeekStart = weekStart(today);
        long scheduledPast = 0;
        long completedPast = 0;
        for (LocalDate weekStart = weekStart(start);
             weekStart.isBefore(currentWeekStart) && !weekStart.isAfter(lastScheduledDay);
             weekStart = weekStart.plusWeeks(1)) {
            LocalDate weekEnd = weekStart.plusDays(6);
            if (weekEnd.isBefore(start)) {
                continue;
            }
            LocalDate from = max(weekStart, start);
            LocalDate to = min(weekEnd, lastScheduledDay);
            if (from.isAfter(to)) {
                continue;
            }
            // A partial week only asks for the days it actually had.
            scheduledPast += Math.min(goal, ChronoUnit.DAYS.between(from, to) + 1);
            completedPast += completionRepository.countByTaskIdAndCompletionDateBetween(task.getId(), from, to);
        }

        long skipped = Math.max(0, scheduledPast - completedPast);
        long scheduled = scheduledPast + goal; // current week still counts its full target
        return new DailyTaskStatisticsDTO(scheduled, completed, skipped, completedToday);
    }

    private LocalDate taskStart(Task task) {
        return asLocalDate(task.getStartDate() != null ? task.getStartDate() : task.getCreatedDate());
    }

    private LocalDate weekStart(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private long countScheduledDays(Task task, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            return 0;
        }
        long count = 0;
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            if (isScheduledOn(task, day)) {
                count++;
            }
        }
        return count;
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
        LocalDate start = taskStart(task);
        if (date.isBefore(start)) {
            return false;
        }
        if (task.getEndDate() != null && date.isAfter(asLocalDate(task.getEndDate()))) {
            return false;
        }
        TaskType type = task.getType() == null ? TaskType.UNIQUE : task.getType();
        return switch (type) {
            case UNIQUE, DAILY, WEEKLY_GOAL -> true;
            case WEEKLY -> task.getRecurrenceDays() != null
                    && task.getRecurrenceDays().contains(date.getDayOfWeek().getValue());
            case MONTHLY -> matchesDayOfMonth(task, date);
        };
    }

    /** Months shorter than the configured day use their last day (e.g. day 31 in February). */
    private boolean matchesDayOfMonth(Task task, LocalDate date) {
        Integer configured = task.getRecurrenceDayOfMonth();
        if (configured == null) {
            return false;
        }
        return date.getDayOfMonth() == Math.min(configured, date.lengthOfMonth());
    }

    private void validateCompletionDate(Task task, LocalDate date) {
        if (date.isAfter(RequestTimeZone.today())) {
            throw new CustomExceptions.InvalidTaskCompletionException("Completion date cannot be in the future");
        }
        if (!isScheduledOn(task, date)) {
            throw new CustomExceptions.InvalidTaskCompletionException(
                    "Completion date must be a scheduled day of the task"
            );
        }
    }

    /** Keeps only the fields used by the task type and rejects incomplete recurrence settings. */
    private void normalizeRecurrence(Task task) {
        TaskType type = task.getType() == null ? TaskType.UNIQUE : task.getType();
        switch (type) {
            case WEEKLY -> {
                List<Integer> days = new ArrayList<>();
                if (task.getRecurrenceDays() != null) {
                    task.getRecurrenceDays().stream()
                            .filter(day -> day != null && day >= 1 && day <= 7)
                            .distinct()
                            .sorted()
                            .forEach(days::add);
                }
                if (days.isEmpty()) {
                    throw new CustomExceptions.InvalidTaskCompletionException(
                            "Weekly tasks need at least one weekday (1 = Monday .. 7 = Sunday)"
                    );
                }
                task.setRecurrenceDays(days);
                task.setRecurrenceDayOfMonth(null);
                task.setWeeklyGoal(null);
            }
            case MONTHLY -> {
                Integer day = task.getRecurrenceDayOfMonth();
                if (day == null || day < 1 || day > 31) {
                    throw new CustomExceptions.InvalidTaskCompletionException(
                            "Monthly tasks need a day of month between 1 and 31"
                    );
                }
                task.setRecurrenceDays(null);
                task.setRecurrenceDayOfMonth(day);
                task.setWeeklyGoal(null);
            }
            case WEEKLY_GOAL -> {
                Integer goal = task.getWeeklyGoal();
                if (goal == null || goal < 1 || goal > 7) {
                    throw new CustomExceptions.InvalidTaskCompletionException(
                            "Weekly goal tasks need a goal between 1 and 7"
                    );
                }
                task.setRecurrenceDays(null);
                task.setRecurrenceDayOfMonth(null);
                task.setWeeklyGoal(goal);
            }
            default -> {
                task.setRecurrenceDays(null);
                task.setRecurrenceDayOfMonth(null);
                task.setWeeklyGoal(null);
            }
        }
    }
}
