package com.edu.silva.budgets.domain.enums;

import lombok.Getter;

@Getter
public enum ClientStatus {
    NEW("new"),
    WORKING("working"),
    DONE("done"),
    DELETED("deleted");

    private final String status;

    ClientStatus(String status) {
        this.status = status;
    }
}
