package com.edu.silva.budgets.domain.dtos.responses;

import com.edu.silva.budgets.domain.dtos.ItemDTO;
import com.edu.silva.budgets.domain.entities.Budget;
import com.edu.silva.budgets.domain.entities.Item;
import com.edu.silva.budgets.domain.enums.BudgetStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BudgetResponseDTO {
    private UUID id;
    private String title;
    private String description;
    private String terms;
    private BudgetStatus status;
    private String observations;
    private int validate;
    private Date createdDate;
    private String clientName;
    private String client;
    private List<ItemDTO> items = new ArrayList<>();
    private double total;

    public BudgetResponseDTO(Budget budget) {
        BeanUtils.copyProperties(budget, this);

        this.clientName = budget.getClient().getName();
        this.client = budget.getClient().getId().toString();

        for (Item item : budget.getItems()) {
            ItemDTO iDTO = new ItemDTO(item);
            this.items.add(iDTO);
            this.total += iDTO.getSubTotal();
        }
    }
}
