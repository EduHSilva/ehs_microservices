package com.silva.edu.finances.domain.dtos.response;

import com.silva.edu.finances.domain.entities.Category;
import com.silva.edu.finances.domain.enums.CategoryClassification;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.BeanUtils;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class CategoryResponseDTO {
    private UUID id;
    private String color;
    private String title;
    private int goal;
    private boolean income;
    private CategoryClassification classification;
    private String defaultKey;

    public CategoryResponseDTO(Category category) {
        BeanUtils.copyProperties(category, this);
        if (category.isCustomized()) this.defaultKey = null;
    }

}
