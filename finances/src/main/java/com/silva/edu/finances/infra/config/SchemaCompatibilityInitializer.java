package com.silva.edu.finances.infra.config;

import com.silva.edu.finances.domain.enums.CategoryClassification;
import com.silva.edu.finances.domain.enums.CategoryStatus;
import com.silva.edu.finances.domain.enums.FinancialAccountType;
import com.silva.edu.finances.domain.enums.GoalMode;
import com.silva.edu.finances.domain.enums.RecurrenceStatus;
import com.silva.edu.finances.domain.enums.RecurrenceType;
import com.silva.edu.finances.domain.enums.TransactionStatus;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class SchemaCompatibilityInitializer implements ApplicationRunner {
    private record EnumCheck(String table, String column, Class<? extends Enum<?>> type) { }

    private static final List<EnumCheck> ENUM_CHECKS = List.of(
            new EnumCheck("category", "classification", CategoryClassification.class),
            new EnumCheck("category", "status", CategoryStatus.class),
            new EnumCheck("transaction", "status", TransactionStatus.class),
            new EnumCheck("recurrences", "status", RecurrenceStatus.class),
            new EnumCheck("recurrences", "type", RecurrenceType.class),
            new EnumCheck("finance_account", "type", FinancialAccountType.class),
            new EnumCheck("monthly_goal", "mode", GoalMode.class)
    );

    private final JdbcTemplate jdbcTemplate;

    public SchemaCompatibilityInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Hibernate's ddl-auto=update does not rewrite enum check constraints created by older versions.
        ENUM_CHECKS.forEach(this::syncEnumCheck);
    }

    private void syncEnumCheck(EnumCheck check) {
        String constraint = check.table() + "_" + check.column() + "_check";
        String allowed = Arrays.stream(check.type().getEnumConstants())
                .map(value -> "'" + value.name() + "'")
                .collect(Collectors.joining(", "));
        jdbcTemplate.execute("ALTER TABLE \"" + check.table() + "\" DROP CONSTRAINT IF EXISTS \"" + constraint + "\"");
        jdbcTemplate.execute("ALTER TABLE \"" + check.table() + "\" ADD CONSTRAINT \"" + constraint + "\" " +
                "CHECK (\"" + check.column() + "\" IS NULL OR \"" + check.column() + "\" IN (" + allowed + "))");
    }
}
