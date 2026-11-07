package com.edu.silva.users.domain.dtos.responses;

import java.util.List;

public record UserStatsResponseDTO(
        long totalUsers,
        long totalCompanies,
        long waitingConfirmation,
        long blockedUsers,
        long usersWithoutCompany,
        List<Point> series
) {
    public record Point(String label, long users, long companies) {
    }
}
