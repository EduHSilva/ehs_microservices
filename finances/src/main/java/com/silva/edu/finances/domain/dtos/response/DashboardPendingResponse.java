package com.silva.edu.finances.domain.dtos.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DashboardPendingResponse(String accountId, int horizonDays, List<Item> items) {
    public record Item(UUID id, String kind, String title, BigDecimal amount, boolean income,
                       LocalDate date, String status) {}
}
