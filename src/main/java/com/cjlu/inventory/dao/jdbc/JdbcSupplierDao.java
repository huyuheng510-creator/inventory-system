package com.cjlu.inventory.dao.jdbc;

import com.cjlu.inventory.dao.ConnectionProvider;
import com.cjlu.inventory.dao.SupplierDao;
import com.cjlu.inventory.model.Supplier;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of {@link SupplierDao}. */
public class JdbcSupplierDao implements SupplierDao {

    private static final String COLUMNS = "id, name, contact_name, phone, email";

    private final ConnectionProvider database;

    public JdbcSupplierDao(ConnectionProvider database) {
        this.database = database;
    }

    @Override
    public Optional<Supplier> findById(String id) {
        return findOne("SELECT " + COLUMNS + " FROM suppliers WHERE id = ?", id);
    }

    @Override
    public Optional<Supplier> findByName(String name) {
        return findOne("SELECT " + COLUMNS + " FROM suppliers WHERE name = ?", name);
    }

    @Override
    public List<Supplier> findAll() {
        return database.withConnection(connection -> {
            List<Supplier> suppliers = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT " + COLUMNS + " FROM suppliers ORDER BY name");
                 ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    suppliers.add(map(result));
                }
            }
            return suppliers;
        });
    }

    @Override
    public void save(Supplier supplier) {
        database.withConnection(connection -> {
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE suppliers SET name = ?, contact_name = ?, phone = ?, email = ? WHERE id = ?")) {
                update.setString(1, supplier.getName());
                update.setString(2, supplier.getContactName());
                update.setString(3, supplier.getPhone());
                update.setString(4, supplier.getEmail());
                update.setString(5, supplier.getId());
                if (update.executeUpdate() > 0) {
                    return null;
                }
            }
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO suppliers (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?)")) {
                insert.setString(1, supplier.getId());
                insert.setString(2, supplier.getName());
                insert.setString(3, supplier.getContactName());
                insert.setString(4, supplier.getPhone());
                insert.setString(5, supplier.getEmail());
                insert.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public void deleteById(String id) {
        JdbcSupport.deleteById(database, "suppliers", id);
    }

    @Override
    public boolean existsById(String id) {
        return JdbcSupport.existsById(database, "suppliers", id);
    }

    @Override
    public int count() {
        return JdbcSupport.count(database, "suppliers");
    }

    private Optional<Supplier> findOne(String sql, String key) {
        return database.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, key);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next() ? Optional.of(map(result)) : Optional.empty();
                }
            }
        });
    }

    private static Supplier map(ResultSet result) throws SQLException {
        return new Supplier(result.getString("id"), result.getString("name"),
                result.getString("contact_name"), result.getString("phone"),
                result.getString("email"));
    }
}
