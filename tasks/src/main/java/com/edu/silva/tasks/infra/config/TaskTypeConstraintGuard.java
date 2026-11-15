package com.edu.silva.tasks.infra.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Bancos criados antes dos tipos recorrentes mantêm o CHECK de {@code tasks.type} só com
 * UNIQUE/DAILY. O {@code ddl-auto=update} do Hibernate não reescreve constraints existentes,
 * então criar uma tarefa WEEKLY/MONTHLY/WEEKLY_GOAL falhava com 500. Recria o CHECK no start.
 */
@Component
public class TaskTypeConstraintGuard implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TaskTypeConstraintGuard.class);

    private static final String TASK_TYPES =
            "('UNIQUE', 'DAILY', 'WEEKLY', 'MONTHLY', 'WEEKLY_GOAL')";

    private final JdbcTemplate jdbcTemplate;

    public TaskTypeConstraintGuard(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws SQLException {
        if (!isPostgres()) {
            return;
        }

        List<String> definitions = jdbcTemplate.queryForList(
                "SELECT pg_get_constraintdef(oid) FROM pg_constraint "
                        + "WHERE conrelid = 'tasks'::regclass AND conname = 'tasks_type_check'",
                String.class
        );
        if (!definitions.isEmpty() && definitions.get(0).contains("WEEKLY_GOAL")) {
            return;
        }

        jdbcTemplate.execute("ALTER TABLE tasks DROP CONSTRAINT IF EXISTS tasks_type_check");
        jdbcTemplate.execute("ALTER TABLE tasks ADD CONSTRAINT tasks_type_check CHECK (type IN " + TASK_TYPES + ")");
        log.info("Recriado o constraint tasks_type_check com os tipos de tarefa atuais");
    }

    private boolean isPostgres() throws SQLException {
        DataSource dataSource = jdbcTemplate.getDataSource();
        if (dataSource == null) {
            return false;
        }
        try (Connection connection = dataSource.getConnection()) {
            return "PostgreSQL".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName());
        }
    }
}
