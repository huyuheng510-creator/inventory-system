package com.cjlu.inventory.dao.jdbc;

import com.cjlu.inventory.dao.ConnectionProvider;
import com.cjlu.inventory.dao.UserDao;
import com.cjlu.inventory.model.User;
import com.cjlu.inventory.model.UserRole;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of {@link UserDao} for the embedded Derby database. */
public class JdbcUserDao implements UserDao {

    private static final String COLUMNS =
            "id, username, password_hash, display_name, role";

    private final ConnectionProvider database;

    public JdbcUserDao(ConnectionProvider database) {
        this.database = database;
    }

    @Override
    public Optional<User> findById(String id) {
        return findOne("SELECT " + COLUMNS + " FROM users WHERE id = ?", id);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return findOne("SELECT " + COLUMNS + " FROM users WHERE username = ?",
                username == null ? null : username.trim().toLowerCase());
    }

    @Override
    public boolean existsByUsername(String username) {
        return database.withConnection(connection -> {
            try (PreparedStatement statement =
                         connection.prepareStatement("SELECT 1 FROM users WHERE username = ?")) {
                statement.setString(1, username == null ? "" : username.trim().toLowerCase());
                try (ResultSet result = statement.executeQuery()) {
                    return result.next();
                }
            }
        });
    }

    @Override
    public List<User> findAll() {
        return database.withConnection(connection -> {
            List<User> users = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT " + COLUMNS + " FROM users ORDER BY username");
                 ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    users.add(map(result));
                }
            }
            return users;
        });
    }

    @Override
    public void save(User user) {
        database.withConnection(connection -> {
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE users SET username = ?, password_hash = ?, display_name = ?, role = ? WHERE id = ?")) {
                update.setString(1, user.getUsername());
                update.setString(2, user.getPasswordHash());
                update.setString(3, user.getDisplayName());
                update.setString(4, user.getRole().name());
                update.setString(5, user.getId());
                if (update.executeUpdate() > 0) {
                    return null;
                }
            }
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO users (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?)")) {
                insert.setString(1, user.getId());
                insert.setString(2, user.getUsername());
                insert.setString(3, user.getPasswordHash());
                insert.setString(4, user.getDisplayName());
                insert.setString(5, user.getRole().name());
                insert.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public void deleteById(String id) {
        JdbcSupport.deleteById(database, "users", id);
    }

    @Override
    public boolean existsById(String id) {
        return JdbcSupport.existsById(database, "users", id);
    }

    @Override
    public int count() {
        return JdbcSupport.count(database, "users");
    }

    private Optional<User> findOne(String sql, String key) {
        return database.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, key);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next() ? Optional.of(map(result)) : Optional.empty();
                }
            }
        });
    }

    private static User map(ResultSet result) throws SQLException {
        return new User(result.getString("id"), result.getString("username"),
                result.getString("password_hash"), result.getString("display_name"),
                UserRole.valueOf(result.getString("role")));
    }
}
