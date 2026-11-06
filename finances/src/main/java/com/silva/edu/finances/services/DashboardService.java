package com.silva.edu.finances.services;

import com.silva.edu.finances.domain.dtos.response.DashboardPendingResponse;

public interface DashboardService {
    DashboardPendingResponse pending(String accountId, int horizonDays);
}
