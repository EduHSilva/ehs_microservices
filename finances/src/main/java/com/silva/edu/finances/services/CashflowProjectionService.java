package com.silva.edu.finances.services;

import com.silva.edu.finances.domain.dtos.response.CashflowProjectionResponse;

public interface CashflowProjectionService {
    CashflowProjectionResponse project(String accountId, int horizonDays);
}
