package com.silva.edu.finances.services;

import com.silva.edu.finances.domain.dtos.response.DashboardPendingResponse;
import com.silva.edu.finances.domain.dtos.response.InvestmentSummaryResponse;

public interface DashboardService {
    DashboardPendingResponse pending(String accountId, int horizonDays);

    InvestmentSummaryResponse investments(String accountId);
}
