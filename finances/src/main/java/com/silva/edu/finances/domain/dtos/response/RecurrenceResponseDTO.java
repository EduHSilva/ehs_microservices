package com.silva.edu.finances.domain.dtos.response;

import com.silva.edu.finances.domain.entities.Recurrence;
import com.silva.edu.finances.domain.enums.CategoryClassification;
import com.silva.edu.finances.domain.enums.RecurrenceStatus;
import com.silva.edu.finances.domain.enums.RecurrenceType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.BeanUtils;

import java.util.UUID;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class RecurrenceResponseDTO {
    private UUID id;
    private String title;
    private String category;
    private UUID categoryID;
    private int day;
    private RecurrenceType type;
    private BigDecimal value;
    private boolean income;
    private Integer installments;
    private Integer payed;
    private RecurrenceStatus status;


    public RecurrenceResponseDTO(Recurrence recurrence) {
        BeanUtils.copyProperties(recurrence, this);
        this.category = recurrence.getCategory().getTitle();
        this.categoryID = recurrence.getCategory().getId();
    }

}
