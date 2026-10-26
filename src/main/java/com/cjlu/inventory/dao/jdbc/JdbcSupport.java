package com.cjlu.inventory.dao.jdbc;

import com.cjlu.inventory.dao.ConnectionProvider;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/** Small shared helpers for the JDBC DAO implementations. */
final class JdbcSupport {

    private JdbcSupport() {
        // utility class
    }

    static void deleteById(ConnectionProvider database, String table, String id) {
        database.withConnection(connection -> {
            try (PreparedStatement statement =
                         connection.prepareStatement("DELETE FROM " + table + " WHERE id = ?")) {
                statement.setString(1, id);
                statement.executeUpdate();
            }
            return null;
        });
    }

    static boolean existsById(ConnectionProvider database, String table, String id) {
        return database.withConnection(connection -> {
            try (PreparedStatement statement =
                         connection.prepareStatement("SELECT 1 FROM " + table + " WHERE id = ?")) {
                statement.setString(1, id);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next();
                }
            }
        });
    }

    static int count(ConnectionProvider database, String table) {
        return database.withConnection(connection -> {
            try (PreparedStatement statement =
                         connection.prepareStatement("SELECT COUNT(*) FROM " + table);
                 ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        });
    }

    static Optional<String> optionalString(ResultSet result, String column) throws SQLException {
        return Optional.ofNullable(result.getString(column));
    }
}
