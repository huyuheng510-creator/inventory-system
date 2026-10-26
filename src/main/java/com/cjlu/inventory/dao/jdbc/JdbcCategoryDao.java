package com.cjlu.inventory.dao.jdbc;

import com.cjlu.inventory.dao.CategoryDao;
import com.cjlu.inventory.dao.ConnectionProvider;
import com.cjlu.inventory.model.Category;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of {@link CategoryDao}. */
public class JdbcCategoryDao implements CategoryDao {

    private static final String COLUMNS = "id, name, description";

    private final ConnectionProvider database;

    public JdbcCategoryDao(ConnectionProvider database) {
        this.database = database;
    }

    @Override
    public Optional<Category> findById(String id) {
        return findOne("SELECT " + COLUMNS + " FROM categories WHERE id = ?", id);
    }

    @Override
    public Optional<Category> findByName(String name) {
        return findOne("SELECT " + COLUMNS + " FROM categories WHERE name = ?", name);
    }

    @Override
    public List<Category> findAll() {
        return database.withConnection(connection -> {
            List<Category> categories = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT " + COLUMNS + " FROM categories ORDER BY name");
                 ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    categories.add(map(result));
                }
            }
            return categories;
        });
    }

    @Override
    public void save(Category category) {
        database.withConnection(connection -> {
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE categories SET name = ?, description = ? WHERE id = ?")) {
                update.setString(1, category.getName());
                update.setString(2, category.getDescription());
                update.setString(3, category.getId());
                if (update.executeUpdate() > 0) {
                    return null;
                }
            }
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO categories (" + COLUMNS + ") VALUES (?, ?, ?)")) {
                insert.setString(1, category.getId());
                insert.setString(2, category.getName());
                insert.setString(3, category.getDescription());
                insert.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public void deleteById(String id) {
        JdbcSupport.deleteById(database, "categories", id);
    }

    @Override
    public boolean existsById(String id) {
        return JdbcSupport.existsById(database, "categories", id);
    }

    @Override
    public int count() {
        return JdbcSupport.count(database, "categories");
    }

    private Optional<Category> findOne(String sql, String key) {
        return database.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, key);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next() ? Optional.of(map(result)) : Optional.empty();
                }
            }
        });
    }

    private static Category map(ResultSet result) throws SQLException {
        return new Category(result.getString("id"), result.getString("name"),
                result.getString("description"));
    }
}
