package com.edu.silva.budgets.domain.dtos;

import com.edu.silva.budgets.domain.entities.Item;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.BeanUtils;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemDTO {
    private UUID id;
    private String name;
    private String description;
    private int quantity;
    private double price;
    private double subTotal;

    public ItemDTO(Item item) {
        BeanUtils.copyProperties(item, this);
        this.price = item.getPriceUn();
        this.subTotal = this.price * this.quantity;
    }
}
