package com.cjlu.inventory.db;

import com.cjlu.inventory.dao.ConnectionProvider;
import com.cjlu.inventory.exception.DataAccessException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates the Derby schema on the very first start.
 *
 * <p>The DDL lives in {@code src/main/resources/db/schema.sql} so it can be
 * inspected, reviewed and compared with the class diagram. The initializer is
 * idempotent: it only runs when the {@code users} table does not exist yet.</p>
 */
public final class SchemaInitializer {

    private static final String SCHEMA_RESOURCE = "/db/schema.sql";

    private SchemaInitializer() {
        // utility class
    }

    /** Creates all tables when the database is empty. */
    public static void ensureSchema(ConnectionProvider provider) {
        provider.withConnection(connection -> {
            if (tableExists(connection, "USERS")) {
                return null;
            }
            for (String statement : schemaStatements()) {
                try (Statement sql = connection.createStatement()) {
                    sql.execute(statement);
                }
            }
            return null;
        });
    }

    private static boolean tableExists(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet tables = metaData.getTables(null, null, tableName, new String[] {"TABLE"})) {
            return tables.next();
        }
    }

    private static List<String> schemaStatements() {
        String script = readSchemaScript();
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : script.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                continue;
            }
            current.append(line).append('\n');
            if (trimmed.endsWith(";")) {
                String statement = current.toString().trim();
                statements.add(statement.substring(0, statement.length() - 1));
                current.setLength(0);
            }
        }
        if (!current.toString().isBlank()) {
            statements.add(current.toString().trim());
        }
        return statements;
    }

    private static String readSchemaScript() {
        try (InputStream stream = SchemaInitializer.class.getResourceAsStream(SCHEMA_RESOURCE)) {
            if (stream == null) {
                throw new DataAccessException("Schema resource " + SCHEMA_RESOURCE + " is missing.");
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DataAccessException("Could not read the database schema.", e);
        }
    }
}
