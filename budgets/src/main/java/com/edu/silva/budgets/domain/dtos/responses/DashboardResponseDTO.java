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
public class DashboardResponseDTO {
    private int totalClients;
    private int pendingServices;
    private int workingServices;
    private double totalReceived;
    private List<BudgetResponseDTO> recentServices = new ArrayList<>();

    public DashboardResponseDTO(int totalClients, int pendingServices, int workingServices, double totalReceived) {
        this.totalClients = totalClients;
        this.pendingServices = pendingServices;
        this.workingServices = workingServices;
        this.totalReceived = totalReceived;
    }
}
