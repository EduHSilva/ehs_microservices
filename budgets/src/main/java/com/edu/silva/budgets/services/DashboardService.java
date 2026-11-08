package com.edu.silva.budgets.services;

import com.edu.silva.budgets.domain.dtos.responses.DashboardKanbanResponseDTO;
import com.edu.silva.budgets.domain.dtos.responses.DashboardResponseDTO;

public interface DashboardService {
    DashboardResponseDTO getData ();
    DashboardKanbanResponseDTO getDataKanban();
}
