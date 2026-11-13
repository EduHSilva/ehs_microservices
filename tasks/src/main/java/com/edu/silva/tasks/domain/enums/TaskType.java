package com.edu.silva.tasks.domain.enums;

/**
 * Defines how a task recurs.
 *
 * <ul>
 *   <li>{@link #UNIQUE} — completed once inside its start/end interval.</li>
 *   <li>{@link #DAILY} — can be completed on every calendar day of the interval.</li>
 *   <li>{@link #WEEKLY} — can be completed on the weekdays listed in {@code recurrenceDays}.</li>
 *   <li>{@link #MONTHLY} — can be completed on {@code recurrenceDayOfMonth} (clamped to the last day of shorter months).</li>
 *   <li>{@link #WEEKLY_GOAL} — can be completed on any day, targeting {@code weeklyGoal} completions per week.</li>
 * </ul>
 */
public enum TaskType {
    UNIQUE,
    DAILY,
    WEEKLY,
    MONTHLY,
    WEEKLY_GOAL;

    /** Whether the task keeps one completion record per day (and therefore has no DELAYED status). */
    public boolean isRecurring() {
        return this != UNIQUE;
    }
}
