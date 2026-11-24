package com.edu.silva.common.sql;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

public final class SqlResourceExecutor {

    private SqlResourceExecutor() {
    }

    public static boolean executeIfPostgres(DataSource dataSource, String resource) throws SQLException, IOException {
        try (Connection connection = dataSource.getConnection()) {
            String product = connection.getMetaData().getDatabaseProductName();
            if (product == null || !product.toLowerCase(Locale.ROOT).contains("postgres")) {
                return false;
            }
            String sql = readResource(resource);
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
            }
            return true;
        }
    }

    static String readResource(String resource) throws IOException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream stream = loader == null ? null : loader.getResourceAsStream(resource);
        if (stream == null) {
            stream = SqlResourceExecutor.class.getClassLoader().getResourceAsStream(resource);
        }
        if (stream == null) {
            throw new IOException("SQL resource not found: " + resource);
        }
        try (InputStream in = stream) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
