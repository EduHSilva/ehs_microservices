package com.silva.edu.finances.domain.enums;

public enum TransactionStatus {
    OK("ok"),
    REVIEW("review"),
    DELAY("delay"),
    DELETED("deleted");

    private final String status;

    TransactionStatus(String status) {
        this.status = status;
    }
}
