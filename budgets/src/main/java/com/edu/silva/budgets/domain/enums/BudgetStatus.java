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
}
