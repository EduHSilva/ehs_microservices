package com.silva.edu.finances.domain.dtos.response;

import com.silva.edu.finances.domain.entities.Transaction;
import com.silva.edu.finances.domain.enums.TransactionStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.BeanUtils;

import java.util.Date;
import java.util.UUID;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class TransactionResponseDTO {
    private UUID id;
    private boolean income;
    private BigDecimal value;
    private String category;
    private UUID categoryID;
    private String categoryKey;
    private String classification;
    private String recurrence;
    private Date executionDate;
    private String title;
    private TransactionStatus status;
    private String accountId;

    public TransactionResponseDTO(Transaction transaction) {
        BeanUtils.copyProperties(transaction, this);
        this.accountId = transaction.getAccountId();
        if (transaction.getCategory() != null) {
            this.category = transaction.getCategory().getTitle();
            this.categoryID = transaction.getCategory().getId();
            this.categoryKey = transaction.getCategory().isCustomized()
                    ? null : transaction.getCategory().getDefaultKey();
            this.classification = transaction.getCategory().getClassification() == null
                    ? null : transaction.getCategory().getClassification().name();
        }
        if (transaction.getRecurrence() != null) {
            this.recurrence = transaction.getRecurrence().getTitle();
        }
    }
}
