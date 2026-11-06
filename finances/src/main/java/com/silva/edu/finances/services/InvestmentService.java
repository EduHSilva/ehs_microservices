package com.silva.edu.finances.services;

import com.silva.edu.finances.domain.dtos.request.InvestmentMovementRequest;
import com.silva.edu.finances.domain.dtos.response.InvestmentMovementResponse;
import com.silva.edu.finances.domain.dtos.response.InvestmentSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface InvestmentService {
    List<InvestmentMovementResponse> list(String accountId);
    InvestmentMovementResponse create(InvestmentMovementRequest request);
    void delete(UUID id);
    InvestmentSummaryResponse summary(String accountId);
}
