package com.edu.silva.budgets.domain.enums;

import lombok.Getter;

@Getter
public enum BudgetStatus {
    DRAFT("draft"),
    SEND("send"),
    WAITING("waiting"),
    APPROVED("approved"),
    WORKING("working"),
    DONE("done"),
    CANCEL("cancel"),
    DENY("deny"),
    DELETED("deleted");

    private final String status;

    BudgetStatus(String status) {
        this.status = status;
    }

    /**
     * Indica se o orçamento pode sair deste status para {@code target}.
     * Manter o mesmo status é sempre permitido (edição sem mudança de etapa);
     * {@code CANCEL} e {@code DELETED} podem ser aplicados a partir de qualquer
     * status.
     */
    public boolean canTransitionTo(BudgetStatus target) {
        if (target == this) {
            return true;
        }
        if (target == CANCEL || target == DELETED) {
            return true;
        }
        return switch (this) {
            // SEND existe apenas por compatibilidade com registros antigos: o
            // fluxo atual vai direto do rascunho para WAITING (envio manual).
            case DRAFT -> target == WAITING || target == SEND;
            case SEND -> target == WAITING || target == APPROVED || target == DENY;
            case WAITING -> target == APPROVED || target == DENY;
            case APPROVED -> target == WORKING;
            case WORKING -> target == DONE;
            case DONE, CANCEL, DENY, DELETED -> false;
        };
    }
}
