package com.edu.silva.budgets.domain.enums;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static com.edu.silva.budgets.domain.enums.BudgetStatus.APPROVED;
import static com.edu.silva.budgets.domain.enums.BudgetStatus.CANCEL;
import static com.edu.silva.budgets.domain.enums.BudgetStatus.DELETED;
import static com.edu.silva.budgets.domain.enums.BudgetStatus.DENY;
import static com.edu.silva.budgets.domain.enums.BudgetStatus.DONE;
import static com.edu.silva.budgets.domain.enums.BudgetStatus.DRAFT;
import static com.edu.silva.budgets.domain.enums.BudgetStatus.SEND;
import static com.edu.silva.budgets.domain.enums.BudgetStatus.WAITING;
import static com.edu.silva.budgets.domain.enums.BudgetStatus.WORKING;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BudgetStatusTest {

    private static final Map<BudgetStatus, Set<BudgetStatus>> ALLOWED = Map.of(
            DRAFT, Set.of(DRAFT, SEND, WAITING, CANCEL, DELETED),
            SEND, Set.of(SEND, WAITING, APPROVED, DENY, CANCEL, DELETED),
            WAITING, Set.of(WAITING, APPROVED, DENY, CANCEL, DELETED),
            APPROVED, Set.of(APPROVED, WORKING, CANCEL, DELETED),
            WORKING, Set.of(WORKING, DONE, CANCEL, DELETED),
            DONE, Set.of(DONE, CANCEL, DELETED),
            DENY, Set.of(DENY, CANCEL, DELETED),
            CANCEL, Set.of(CANCEL, DELETED),
            DELETED, Set.of(DELETED, CANCEL)
    );

    @Test
    void shouldEnforceTransitionMatrix() {
        for (BudgetStatus from : BudgetStatus.values()) {
            for (BudgetStatus to : BudgetStatus.values()) {
                assertEquals(
                        ALLOWED.get(from).contains(to),
                        from.canTransitionTo(to),
                        from + " -> " + to
                );
            }
        }
    }
}
