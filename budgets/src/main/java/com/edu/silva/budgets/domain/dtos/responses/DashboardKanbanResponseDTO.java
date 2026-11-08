package com.edu.silva.budgets.domain.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DashboardKanbanResponseDTO {
    private List<BudgetResponseDTO> news = new ArrayList<>();
    private List<BudgetResponseDTO> working = new ArrayList<>();
    private List<BudgetResponseDTO> done = new ArrayList<>();
}
