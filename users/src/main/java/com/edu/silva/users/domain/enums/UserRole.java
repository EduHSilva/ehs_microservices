package com.edu.silva.users.domain.enums;

import lombok.Getter;

@Getter
public enum UserRole {
    ADMIN("admin"),
    BUDGETS("budgets"),
    FINANCES("finances"),
    HEALTH_USER("health_user"),
    HEALTH_PERSONAL("health_personal"),
    HEALTH_NUTRITIONIST("health_nutritionist"),
    STUDY("study");

    private final String role;

    UserRole(String role) {
        this.role = role;
    }
}
