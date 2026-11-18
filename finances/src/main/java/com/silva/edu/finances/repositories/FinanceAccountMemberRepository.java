package com.silva.edu.finances.repositories;

import com.silva.edu.finances.domain.entities.FinanceAccountMember;
import com.silva.edu.finances.domain.enums.AccountMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FinanceAccountMemberRepository extends JpaRepository<FinanceAccountMember, UUID> {
    Optional<FinanceAccountMember> findByIdAndAccountId(UUID id, String accountId);
    Optional<FinanceAccountMember> findByToken(String token);
    Optional<FinanceAccountMember> findByAccountIdAndEmail(String accountId, String email);
    Optional<FinanceAccountMember> findByAccountIdAndUserId(String accountId, UUID userId);
    List<FinanceAccountMember> findAllByAccountIdAndStatusInOrderByCreatedAtAsc(String accountId, List<AccountMemberStatus> statuses);
    List<FinanceAccountMember> findAllByUserIdAndStatus(UUID userId, AccountMemberStatus status);
    List<FinanceAccountMember> findAllByAccountIdInAndStatus(Collection<String> accountIds, AccountMemberStatus status);
    List<FinanceAccountMember> findAllByEmailAndStatus(String email, AccountMemberStatus status);
    boolean existsByAccountIdAndUserIdAndStatus(String accountId, UUID userId, AccountMemberStatus status);

    @Modifying
    @Query("update FinanceAccountMember m set m.status = com.silva.edu.finances.domain.enums.AccountMemberStatus.EXPIRED, m.token = null where m.status = com.silva.edu.finances.domain.enums.AccountMemberStatus.PENDING and m.expiresAt < :now")
    int expireDue(@Param("now") LocalDateTime now);
}
