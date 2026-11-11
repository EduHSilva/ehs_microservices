package com.edu.silva.tasks.domain.enums;

import lombok.Getter;

@Getter
public enum TaskStatus {
    PENDING("pending"),
    WORKING("working"),
    DONE("done"),
    DELAYED("delayed"),
    DELETED("deleted");

    private final String status;

    TaskStatus(String status) {
        this.status = status;
    }
}
