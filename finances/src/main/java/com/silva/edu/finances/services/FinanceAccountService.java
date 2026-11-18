package com.silva.edu.finances.services;

import com.silva.edu.finances.domain.dtos.request.CreateFinanceAccountRequest;
import com.silva.edu.finances.domain.dtos.response.FinanceAccountResponse;
import com.silva.edu.finances.domain.entities.FinanceAccount;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface FinanceAccountService {
    List<FinanceAccountResponse> listMine();
    FinanceAccountResponse create(CreateFinanceAccountRequest request);
    FinanceAccountResponse get(String accountId);
    void requireAccess(String accountId);
    FinanceAccount requireAccount(String accountId);
    UUID ownerOf(String accountId);
    String getOrCreatePersonalDefault();
    List<String> accessibleAccountIds();
    Set<UUID> visibleUserIds();
}
