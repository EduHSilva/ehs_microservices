package com.silva.edu.finances.domain.enums;

public enum CategoryClassification {
    ESSENTIAL("essential"),
    RECREATION("recreation"),
    INVESTMENT("investment");


    private final String classification;

    CategoryClassification(String c) {
        this.classification = c;
    }
}
