package com.edu.silva.common.sql;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Confere, contra o schema real, se a função lgpd_purge_user cobre todas as tabelas que
 * guardam dados do usuário. Lê as listas direto do SQL para não duplicar a fonte de verdade.
 * Retorna uma lista de problemas (vazia quando está tudo coberto); não altera nada.
 */
public final class LgpdCoverageVerifier {

    private static final Pattern COVERED = Pattern.compile(
            "v_covered\\s+(?:text\\[\\]\\s*)?:?=\\s*ARRAY\\s*\\[(.*?)]", Pattern.DOTALL);
    private static final Pattern OWNER_COLUMNS = Pattern.compile(
            "column_name\\s+IN\\s*\\((.*?)\\)", Pattern.DOTALL);
    private static final Pattern QUOTED = Pattern.compile("'([^']+)'");
    private static final Set<String> IGNORED_TABLES = Set.of(
            "flyway_schema_history", "databasechangelog", "databasechangeloglock", "hibernate_sequence");

    private LgpdCoverageVerifier() {
    }

    public static List<String> verify(DataSource dataSource, String resource, Set<String> extraCovered)
            throws SQLException, java.io.IOException {
        String sql = SqlResourceExecutor.readResource(resource);

        Set<String> covered = parseQuoted(sql, COVERED, resource, "v_covered");
        covered.addAll(extraCovered);
        Set<String> ownerColumns = parseQuoted(sql, OWNER_COLUMNS, resource, "lista de colunas de dono");
        List<String> problems = new ArrayList<>();
        if (covered.isEmpty()) {
            problems.add("não foi possível ler v_covered em " + resource);
            return problems;
        }
        if (ownerColumns.isEmpty()) {
            problems.add("não foi possível ler a lista de colunas de dono em " + resource);
            return problems;
        }

        try (Connection connection = dataSource.getConnection()) {
            String product = connection.getMetaData().getDatabaseProductName();
            if (product == null || !(product.toLowerCase(Locale.ROOT).contains("postgres")
                    || product.toLowerCase(Locale.ROOT).equals("h2"))) {
                return problems;
            }
            String schema = connection.getSchema();
            if (schema == null || schema.isBlank()) {
                schema = product.equalsIgnoreCase("h2") ? "PUBLIC" : "public";
            }

            Map<String, String> tables = listTables(connection, schema);
            for (Map.Entry<String, String> entry : tables.entrySet()) {
                String table = entry.getKey();
                if (covered.contains(table)) {
                    continue;
                }
                for (String column : listColumns(connection, schema, entry.getValue())) {
                    if (ownerColumns.contains(column)) {
                        problems.add("tabela " + table + " tem coluna de dono " + column
                                + " e não está coberta por lgpd_purge_user");
                    }
                }
                for (String parent : importedParents(connection, schema, entry.getValue())) {
                    if (covered.contains(parent)) {
                        problems.add("tabela " + table + " referencia a tabela coberta " + parent
                                + " por FK e não está coberta por lgpd_purge_user");
                    }
                }
            }
            for (String table : covered) {
                if (!tables.containsKey(table)) {
                    problems.add("tabela coberta " + table + " não existe no schema (renomeada ou removida?)");
                }
            }
        }
        return problems;
    }

    private static Set<String> parseQuoted(String sql, Pattern pattern, String resource, String what) {
        Matcher matcher = pattern.matcher(sql);
        Set<String> values = new TreeSet<>();
        if (!matcher.find()) {
            return values;
        }
        Matcher quoted = QUOTED.matcher(matcher.group(1));
        while (quoted.find()) {
            values.add(quoted.group(1).toLowerCase(Locale.ROOT));
        }
        return values;
    }

    private static Map<String, String> listTables(Connection connection, String schema) throws SQLException {
        Map<String, String> tables = new LinkedHashMap<>();
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet result = metaData.getTables(null, schema, "%", new String[]{"TABLE"})) {
            while (result.next()) {
                String name = result.getString("TABLE_NAME");
                String normalized = name.toLowerCase(Locale.ROOT);
                if (!IGNORED_TABLES.contains(normalized)) {
                    tables.put(normalized, name);
                }
            }
        }
        return tables;
    }

    private static Set<String> listColumns(Connection connection, String schema, String table)
            throws SQLException {
        Set<String> columns = new TreeSet<>();
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet result = metaData.getColumns(null, schema, table, "%")) {
            while (result.next()) {
                columns.add(result.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
            }
        }
        return columns;
    }

    private static Set<String> importedParents(Connection connection, String schema, String table)
            throws SQLException {
        Set<String> parents = new TreeSet<>();
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet result = metaData.getImportedKeys(null, schema, table)) {
            while (result.next()) {
                parents.add(result.getString("PKTABLE_NAME").toLowerCase(Locale.ROOT));
            }
        }
        return parents;
    }
}
