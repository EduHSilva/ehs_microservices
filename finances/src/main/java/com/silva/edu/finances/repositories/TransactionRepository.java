package com.silva.edu.finances.repositories;

import com.silva.edu.finances.domain.entities.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.util.Date;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);
    Page<Transaction> findAllByAccountId(String accountId, Pageable pageable);
    @Query("select coalesce(sum(t.value), 0) from Transaction t where t.userId = :userId and t.accountId = :accountId and t.income = true and t.status = com.silva.edu.finances.domain.enums.TransactionStatus.OK and t.executionDate >= :start and t.executionDate < :end")
    BigDecimal sumConfirmedIncome(@Param("userId") UUID userId, @Param("accountId") String accountId,
                                  @Param("start") Date start, @Param("end") Date end);
    @Query(value = "select exists(select 1 from transaction where user_id = :userId and account_id = :accountId and import_key = :key)", nativeQuery = true)
    boolean importKeyExists(@Param("userId") UUID userId, @Param("accountId") String accountId,
                            @Param("key") String key);
    @Query("select coalesce(sum(case when t.income = true then t.value else -t.value end), 0) from Transaction t where t.userId = :userId and t.accountId = :accountId and t.status = com.silva.edu.finances.domain.enums.TransactionStatus.OK and t.executionDate < :asOf")
    BigDecimal sumNetByAccountAndUser(@Param("accountId") String accountId, @Param("userId") UUID userId,
                                      @Param("asOf") Date asOf);
    org.springframework.data.domain.Page<Transaction> findAllByAccountIdAndExecutionDateBetween(
            String accountId, Date start, Date end, Pageable pageable);
    List<Transaction> findAllByAccountIdAndStatusAndExecutionDateLessThanEqualOrderByExecutionDateAsc(
            String accountId, com.silva.edu.finances.domain.enums.TransactionStatus status, Date to);
    @Query("select t from Transaction t where t.accountId = :accountId and t.recurrence is not null and t.executionDate >= :start and t.executionDate < :end")
    List<Transaction> findAllRecurrenceLinkedByAccountId(@Param("accountId") String accountId,
                                                         @Param("start") Date start, @Param("end") Date end);
    @Query("select t from Transaction t where t.recurrence.id = :recurrenceId and t.userId = :userId and t.executionDate >= :start and t.executionDate < :end order by t.executionDate asc")
    List<Transaction> findRecurrenceOccurrences(@Param("recurrenceId") UUID recurrenceId,
                                                @Param("userId") UUID userId,
                                                @Param("start") Date start, @Param("end") Date end);
    @org.springframework.data.jpa.repository.Modifying
    @Query("update Transaction t set t.status = com.silva.edu.finances.domain.enums.TransactionStatus.DELETED where t.recurrence.id = :recurrenceId and t.userId = :userId and t.status = com.silva.edu.finances.domain.enums.TransactionStatus.PENDING")
    int deletePendingByRecurrence(@Param("recurrenceId") UUID recurrenceId, @Param("userId") UUID userId);
    @Query("select t.income, coalesce(sum(t.value), 0) from Transaction t where t.userId = :userId and t.accountId = :accountId and t.status = com.silva.edu.finances.domain.enums.TransactionStatus.OK and t.category.classification = :classification and t.executionDate < :asOf group by t.income")
    List<Object[]> sumByClassification(@Param("userId") UUID userId, @Param("accountId") String accountId,
                                       @Param("classification") com.silva.edu.finances.domain.enums.CategoryClassification classification,
                                       @Param("asOf") Date asOf);
    @Query("select t.income, coalesce(sum(t.value), 0), count(t.id) from Transaction t where t.userId = :userId and t.accountId = :accountId and t.status = com.silva.edu.finances.domain.enums.TransactionStatus.OK and t.recurrence is null and t.executionDate >= :start and t.executionDate < :end group by t.income")
    List<Object[]> sumVariableHistory(@Param("userId") UUID userId, @Param("accountId") String accountId,
                                      @Param("start") Date start, @Param("end") Date end);
    @Modifying
    @Query("update Transaction t set t.accountId = :accountId where t.userId = :userId and t.accountId is null")
    int assignLegacyTransactions(@Param("userId") UUID userId, @Param("accountId") String accountId);
    org.springframework.data.domain.Page<Transaction> findAllByUserIdAndRecurrenceIsNotNull(
            UUID userId, Pageable pageable);
}
