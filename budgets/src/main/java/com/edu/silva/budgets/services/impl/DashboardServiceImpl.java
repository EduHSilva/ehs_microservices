package com.edu.silva.budgets.services.impl;

import com.edu.silva.budgets.domain.dtos.responses.BudgetResponseDTO;
import com.edu.silva.budgets.domain.dtos.responses.DashboardKanbanResponseDTO;
import com.edu.silva.budgets.domain.dtos.responses.DashboardResponseDTO;
import com.edu.silva.budgets.domain.entities.Budget;
import com.edu.silva.budgets.domain.enums.BudgetStatus;
import com.edu.silva.budgets.infra.config.TenantFilter;
import com.edu.silva.budgets.repositories.BudgetRepository;
import com.edu.silva.budgets.services.DashboardService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final JdbcTemplate jdbc;
    private final BudgetRepository repository;
    private final TenantFilter tenantFilter;

    public DashboardServiceImpl(JdbcTemplate jdbc, BudgetRepository repository, TenantFilter tenantFilter) {
        this.jdbc = jdbc;
        this.repository = repository;
        this.tenantFilter = tenantFilter;
    }

    @Override
    public DashboardResponseDTO getData() {
        tenantFilter.apply();
        UUID userId = tenantFilter.currentUserId();

        String sql = """
                  SELECT
                  COUNT(DISTINCT c.id) AS totalClients,
                  COUNT(DISTINCT b.id) FILTER (WHERE b.status = 'APPROVED') AS pendingServices,
                  COUNT(DISTINCT b.id) FILTER (WHERE b.status = 'WORKING') AS workingServices,
                  COALESCE(SUM(i.price_un * i.quantity) FILTER (WHERE b.status = 'DONE'), 0) AS totalRecieved
                FROM clients c
                LEFT JOIN budgets b ON b.client_id = c.id AND b.user_id = CAST(? AS uuid) AND b.status <> 'DELETED'
                LEFT JOIN item i ON i.budget_id = b.id AND i.status <> 'DELETED'
                WHERE c.user_id = CAST(? AS uuid) AND c.status <> 'DELETED';
                """;

        DashboardResponseDTO dto = jdbc.query(sql, (rs, rowNum) ->
                new DashboardResponseDTO(
                        rs.getInt("totalClients"),
                        rs.getInt("pendingServices"),
                        rs.getInt("workingServices"),
                        rs.getDouble("totalRecieved")
                ), userId.toString(), userId.toString()
        ).getFirst();


        dto.setRecentServices(repository.findTop5ByOrderByCreatedDateDesc().stream().map(BudgetResponseDTO::new).toList());

        return dto;
    }

    @Override
    public DashboardKanbanResponseDTO getDataKanban() {
        tenantFilter.apply();
        DashboardKanbanResponseDTO dto = new DashboardKanbanResponseDTO();
        List<Budget> news = repository.findByStatusOrderByKanbanOrder(BudgetStatus.APPROVED);
        List<Budget> working = repository.findByStatusOrderByKanbanOrder(BudgetStatus.WORKING);
        List<Budget> done = repository.findByStatusOrderByKanbanOrder(BudgetStatus.DONE);
        dto.setNews(news.stream().map(BudgetResponseDTO::new).toList());
        dto.setWorking(working.stream().map(BudgetResponseDTO::new).toList());
        dto.setDone(done.stream().map(BudgetResponseDTO::new).toList());
        return dto;
    }
}
