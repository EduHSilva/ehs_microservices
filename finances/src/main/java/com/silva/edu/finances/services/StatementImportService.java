package com.silva.edu.finances.services;

import com.silva.edu.finances.domain.dtos.request.StatementImportRequest;
import com.silva.edu.finances.domain.dtos.response.StatementImportPreviewResponse;
import com.silva.edu.finances.domain.dtos.response.StatementImportResultResponse;
import com.silva.edu.finances.domain.dtos.request.ReviewImportedTransactionRequest;
import com.silva.edu.finances.domain.dtos.response.TransactionResponseDTO;
import com.silva.edu.finances.domain.dtos.response.TransactionImportAuditResponse;
import java.util.UUID;

public interface StatementImportService {
    StatementImportPreviewResponse preview(StatementImportRequest request);
    StatementImportResultResponse confirm(StatementImportRequest request);
    TransactionResponseDTO review(UUID transactionId, ReviewImportedTransactionRequest request);
    java.util.List<TransactionImportAuditResponse> audit(UUID transactionId);
}
