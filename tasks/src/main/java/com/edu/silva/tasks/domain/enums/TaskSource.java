package com.edu.silva.tasks.domain.enums;

/**
 * Aplicação de origem da tarefa. O serviço {@code tasks} é compartilhado pelo StudyFlow
 * ({@link #STUDY}) e pelo Orçamentos ({@link #BUDGETS}, onde a tarefa é um lembrete).
 * Cada produto só enxerga as tarefas criadas por ele.
 */
public enum TaskSource {
    STUDY,
    BUDGETS
}
