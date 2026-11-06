package com.silva.edu.finances.domain.enums;

public enum RecurrenceStatus {
    ACTIVE("active"),
    PENDING("pending"),
    INACTIVE("inactive"),
    PAYED("payed"),
    DELETED("deleted");


    private final String status;

    RecurrenceStatus(String status) {
        this.status = status;
    }
}
