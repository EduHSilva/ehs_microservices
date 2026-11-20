package com.silva.edu.finances.services;

import com.silva.edu.finances.domain.dtos.request.TransferRequest;
import com.silva.edu.finances.domain.dtos.request.UpdateTransferRequest;
import com.silva.edu.finances.domain.dtos.response.TransferResponse;

import java.util.UUID;

public interface TransferService {
    TransferResponse create(TransferRequest request);
    TransferResponse update(UUID transferId, UpdateTransferRequest request);
    void delete(UUID transferId);
    TransferResponse find(UUID transferId);
}
