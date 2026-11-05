package com.silva.edu.finances.services;

import com.silva.edu.finances.domain.dtos.request.GoalContributionRequest;
import com.silva.edu.finances.domain.dtos.request.MonthlyGoalRequest;
import com.silva.edu.finances.domain.dtos.response.MonthlyGoalResponse;
import com.silva.edu.finances.domain.dtos.response.MonthlyGoalSummaryResponse;
import org.springframework.data.domain.Page;
import java.util.UUID;

public interface MonthlyGoalService {
    Page<MonthlyGoalResponse> list(String accountId, String periodMonth, int page, int size);
    MonthlyGoalResponse create(MonthlyGoalRequest request);
    MonthlyGoalResponse update(UUID id, MonthlyGoalRequest request);
    void delete(UUID id);
    MonthlyGoalSummaryResponse summary(UUID id, String periodMonth);
    void contribute(UUID id, GoalContributionRequest request);
    void removeContribution(UUID contributionId);
}
