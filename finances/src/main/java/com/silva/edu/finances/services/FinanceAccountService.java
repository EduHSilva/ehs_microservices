package com.silva.edu.finances.services;

import com.silva.edu.finances.domain.dtos.request.CreateFinanceAccountRequest;
import com.silva.edu.finances.domain.dtos.response.FinanceAccountResponse;
import java.util.List;

public interface FinanceAccountService {
    List<FinanceAccountResponse> listMine();
    FinanceAccountResponse create(CreateFinanceAccountRequest request);
    FinanceAccountResponse get(String accountId);
    void requireAccess(String accountId);
    String getOrCreatePersonalDefault();
}
