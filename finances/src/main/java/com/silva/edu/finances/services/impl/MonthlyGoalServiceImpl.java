package com.silva.edu.finances.services.impl;

import com.silva.edu.finances.domain.dtos.request.GoalContributionRequest;
import com.silva.edu.finances.domain.dtos.request.MonthlyGoalRequest;
import com.silva.edu.finances.domain.dtos.response.MonthlyGoalResponse;
import com.silva.edu.finances.domain.dtos.response.MonthlyGoalSummaryResponse;
import com.silva.edu.finances.domain.entities.Category;
import com.silva.edu.finances.domain.entities.GoalContribution;
import com.silva.edu.finances.domain.entities.MonthlyGoal;
import com.silva.edu.finances.domain.entities.Transaction;
import com.silva.edu.finances.domain.enums.GoalMode;
import com.silva.edu.finances.domain.enums.TransactionStatus;
import com.silva.edu.finances.infra.config.UserContext;
import com.silva.edu.finances.infra.exceptions.CustomExceptions;
import com.silva.edu.finances.repositories.GoalContributionRepository;
import com.silva.edu.finances.repositories.MonthlyGoalRepository;
import com.silva.edu.finances.repositories.TransactionRepository;
import com.silva.edu.finances.services.MonthlyGoalService;
import com.silva.edu.finances.services.CategoryService;
import com.silva.edu.finances.services.FinanceAccountService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class MonthlyGoalServiceImpl implements MonthlyGoalService {
    private final MonthlyGoalRepository goals;
    private final GoalContributionRepository contributions;
    private final TransactionRepository transactions;
    private final FinanceAccountService accountService;
    private final CategoryService categories;

    public MonthlyGoalServiceImpl(MonthlyGoalRepository goals, GoalContributionRepository contributions,
                                  TransactionRepository transactions, FinanceAccountService accountService,
                                  CategoryService categories) {
        this.goals = goals;
        this.contributions = contributions;
        this.transactions = transactions;
        this.accountService = accountService;
        this.categories = categories;
    }

    @Override @Transactional(readOnly = true)
    public Page<MonthlyGoalResponse> list(String accountId, String periodMonth, int page, int size) {
        accountService.requireAccess(accountId);
        YearMonth month = YearMonth.parse(periodMonth);
        return goals.findApplicable(UserContext.getUser(), accountId, periodMonth,
                month.atDay(1), month.atEndOfMonth(), PageRequest.of(page, size)).map(this::response);
    }

    @Override public MonthlyGoalResponse create(MonthlyGoalRequest request) {
        validate(request);
        accountService.requireAccess(request.accountId());
        MonthlyGoal goal = new MonthlyGoal();
        goal.setUserId(UserContext.getUser());
        copy(request, goal);
        return response(goals.save(goal));
    }

    @Override public MonthlyGoalResponse update(UUID id, MonthlyGoalRequest request) {
        validate(request);
        accountService.requireAccess(request.accountId());
        MonthlyGoal goal = findGoal(id);
        copy(request, goal);
        return response(goals.save(goal));
    }

    @Override public void delete(UUID id) {
        MonthlyGoal goal = findGoal(id);
        UUID userId = UserContext.getUser();
        contributions.findAllByGoalIdAndUserId(id, userId).forEach(this::deleteGeneratedTransaction);
        contributions.deleteAllByGoalIdAndUserId(id, userId);
        goals.delete(goal);
    }

    @Override @Transactional(readOnly = true)
    public MonthlyGoalSummaryResponse summary(UUID id, String periodMonth) {
        MonthlyGoal goal = findGoal(id);
        accountService.requireAccess(goal.getAccountId());
        YearMonth month = YearMonth.parse(periodMonth);
        LocalDate start = month.atDay(1);
        LocalDate end = month.plusMonths(1).atDay(1);
        if (!appliesTo(goal, start, month.atEndOfMonth()))
            throw new IllegalArgumentException("Goal does not apply to the requested month");
        BigDecimal base = transactions.sumConfirmedIncome(goal.getUserId(), goal.getAccountId(),
                java.util.Date.from(start.atStartOfDay().toInstant(ZoneOffset.UTC)),
                java.util.Date.from(end.atStartOfDay().toInstant(ZoneOffset.UTC)));
        if (base == null) base = BigDecimal.ZERO;
        BigDecimal expected = goal.getMode() == GoalMode.FIXED ? goal.getAmount()
                : base.multiply(goal.getAmount()).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal realized = contributions.sumForGoalAndPeriod(id, goal.getUserId(), start, end);
        if (realized == null) realized = BigDecimal.ZERO;
        BigDecimal difference = expected.subtract(realized);
        BigDecimal progress = expected.signum() == 0 ? BigDecimal.ZERO
                : realized.multiply(BigDecimal.valueOf(100)).divide(expected, 2, RoundingMode.HALF_UP);

        BigDecimal initialBalance = goal.getInitialBalance() == null ? BigDecimal.ZERO : goal.getInitialBalance();
        BigDecimal objective = goal.getObjective();
        LocalDate savingStart = goal.getDtInicio() != null ? goal.getDtInicio()
                : YearMonth.parse(goal.getPeriodMonth()).atDay(1);
        BigDecimal accumulated = contributions.sumForGoalAndPeriod(id, goal.getUserId(), savingStart, end);
        if (accumulated == null) accumulated = BigDecimal.ZERO;
        BigDecimal saved = initialBalance.add(accumulated);
        BigDecimal objectiveProgress = objective == null || objective.signum() == 0 ? BigDecimal.ZERO
                : saved.multiply(BigDecimal.valueOf(100)).divide(objective, 2, RoundingMode.HALF_UP);

        return new MonthlyGoalSummaryResponse(id, goal.getAccountId(), goal.getTitle(), periodMonth,
                base, expected, realized, difference, progress, initialBalance, objective, saved, objectiveProgress);
    }

    @Override public void contribute(UUID id, GoalContributionRequest request) {
        MonthlyGoal goal = findGoal(id);
        accountService.requireAccess(goal.getAccountId());
        UUID userId = UserContext.getUser();
        GoalContribution contribution = new GoalContribution();
        contribution.setUserId(userId);
        contribution.setGoal(goal);
        contribution.setAmount(request.amount());
        contribution.setDate(request.date());
        if (request.transactionId() != null) {
            if (contributions.existsByTransactionIdAndUserId(request.transactionId(), userId))
                throw new IllegalArgumentException("Transaction already linked to a goal contribution");
            var transaction = transactions.findByIdAndUserId(request.transactionId(), userId)
                    .orElseThrow(() -> new CustomExceptions.EntityNotFoundException("Transaction", request.transactionId()));
            if (!goal.getAccountId().equals(transaction.getAccountId()))
                throw new IllegalArgumentException("Transaction and goal accounts do not match");
            if (transaction.isIncome() || transaction.getStatus() != TransactionStatus.OK
                    || transaction.getValue().compareTo(request.amount()) != 0
                    || transaction.getExecutionDate().toInstant().atZone(ZoneOffset.UTC).toLocalDate().compareTo(request.date()) != 0)
                throw new IllegalArgumentException("Linked contribution must match a confirmed expense transaction");
            contribution.setTransactionId(transaction.getId());
        } else {
            contribution.setTransactionId(saveContributionTransaction(goal, request, userId).getId());
            contribution.setAutoCreated(true);
        }
        contributions.save(contribution);
    }

    @Override public void removeContribution(UUID contributionId) {
        GoalContribution contribution = contributions.findByIdAndUserId(contributionId, UserContext.getUser())
                .orElseThrow(() -> new CustomExceptions.EntityNotFoundException("GoalContribution", contributionId));
        deleteGeneratedTransaction(contribution);
        contributions.delete(contribution);
    }

    private Transaction saveContributionTransaction(MonthlyGoal goal, GoalContributionRequest request, UUID userId) {
        Category investmentCategory = categories.ensureInvestmentCategory(userId);
        Transaction transaction = new Transaction();
        transaction.setUserId(userId);
        transaction.setAccountId(goal.getAccountId());
        transaction.setTitle("Aporte: " + goal.getTitle());
        transaction.setValue(request.amount());
        transaction.setIncome(false);
        transaction.setCategory(investmentCategory);
        transaction.setExecutionDate(java.util.Date.from(request.date().atStartOfDay(ZoneOffset.UTC).toInstant()));
        transaction.setStatus(TransactionStatus.OK);
        return transactions.save(transaction);
    }

    private void deleteGeneratedTransaction(GoalContribution contribution) {
        if (!contribution.isAutoCreated() || contribution.getTransactionId() == null) return;
        transactions.findByIdAndUserId(contribution.getTransactionId(), contribution.getUserId())
                .ifPresent(transactions::delete);
    }

    private MonthlyGoal findGoal(UUID id) {
        return goals.findByIdAndUserId(id, UserContext.getUser())
                .orElseThrow(() -> new CustomExceptions.EntityNotFoundException("MonthlyGoal", id));
    }
    private void validate(MonthlyGoalRequest request) {
        if (request.mode() == GoalMode.PERCENT_INCOME && request.amount().compareTo(BigDecimal.valueOf(100)) > 0)
            throw new IllegalArgumentException("Income percentage must be between 0 and 100");
        if (request.dtInicio() == null && request.periodMonth() == null)
            throw new IllegalArgumentException("Either periodMonth or dtInicio is required");
        if (request.dtInicio() != null && request.dtFim() != null && request.dtFim().isBefore(request.dtInicio()))
            throw new IllegalArgumentException("dtFim cannot be before dtInicio");
    }
    private void copy(MonthlyGoalRequest request, MonthlyGoal goal) {
        YearMonth month = request.dtInicio() != null
                ? YearMonth.from(request.dtInicio()) : YearMonth.parse(request.periodMonth());
        LocalDate start = request.dtInicio() != null ? request.dtInicio() : month.atDay(1);
        LocalDate end;
        if (request.dtInicio() != null) {
            end = request.dtFim();
        } else {
            end = request.dtFim() != null ? request.dtFim() : month.atEndOfMonth();
        }
        goal.setAccountId(request.accountId().trim());
        goal.setTitle(request.title().trim());
        goal.setMode(request.mode());
        goal.setAmount(request.amount());
        goal.setInitialBalance(request.initialBalance() == null ? BigDecimal.ZERO : request.initialBalance());
        goal.setObjective(request.objective() == null || request.objective().signum() <= 0
                ? null : request.objective());
        goal.setPeriodMonth(month.toString());
        goal.setDtInicio(start);
        goal.setDtFim(end);
        goal.setActive(request.active());
    }
    private boolean appliesTo(MonthlyGoal goal, LocalDate start, LocalDate end) {
        if (goal.getDtInicio() == null) return goal.getPeriodMonth().equals(YearMonth.from(start).toString());
        return !goal.getDtInicio().isAfter(end) && (goal.getDtFim() == null || !goal.getDtFim().isBefore(start));
    }
    private MonthlyGoalResponse response(MonthlyGoal g) {
        YearMonth legacyMonth = YearMonth.parse(g.getPeriodMonth());
        LocalDate start = g.getDtInicio() != null ? g.getDtInicio() : legacyMonth.atDay(1);
        LocalDate end = g.getDtFim() != null ? g.getDtFim()
                : (g.getDtInicio() == null ? legacyMonth.atEndOfMonth() : null);
        boolean recurring = end == null || YearMonth.from(end).isAfter(YearMonth.from(start));
        BigDecimal initialBalance = g.getInitialBalance() == null ? BigDecimal.ZERO : g.getInitialBalance();
        return new MonthlyGoalResponse(g.getId(), g.getAccountId(), g.getTitle(), g.getMode(),
                g.getAmount(), initialBalance, g.getObjective(), g.getPeriodMonth(), start, end, recurring, g.isActive());
    }
}
