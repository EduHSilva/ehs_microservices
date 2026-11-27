package com.edu.silva.tasks.domain.enums;

/**
 * Prioridade da tarefa. O StudyFlow não usa o campo (toda tarefa nasce {@link #MEDIUM});
 * o Orçamentos usa para ordenar os lembretes.
 */
public enum TaskPriority {
    LOW,
    MEDIUM,
    HIGH
}
