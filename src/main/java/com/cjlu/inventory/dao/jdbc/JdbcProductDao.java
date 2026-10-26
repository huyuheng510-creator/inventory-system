package com.cjlu.inventory.dao.jdbc;

import com.cjlu.inventory.dao.ConnectionProvider;
import com.cjlu.inventory.dao.ProductDao;
import com.cjlu.inventory.model.Product;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of {@link ProductDao}. */
public class JdbcProductDao implements ProductDao {

    private static final String COLUMNS =
            "id, sku, name, category_id, supplier_id, unit_price, quantity, reorder_level, unit";

    private final ConnectionProvider database;

    public JdbcProductDao(ConnectionProvider database) {
        this.database = database;
    }

    @Override
    public Optional<Product> findById(String id) {
        return findOne("SELECT " + COLUMNS + " FROM products WHERE id = ?", id);
    }

    @Override
    public Optional<Product> findBySku(String sku) {
        return findOne("SELECT " + COLUMNS + " FROM products WHERE sku = ?",
                sku == null ? null : sku.trim().toUpperCase());
    }

    @Override
    public boolean existsBySku(String sku) {
        return database.withConnection(connection -> {
            try (PreparedStatement statement =
                         connection.prepareStatement("SELECT 1 FROM products WHERE sku = ?")) {
                statement.setString(1, sku == null ? "" : sku.trim().toUpperCase());
                try (ResultSet result = statement.executeQuery()) {
                    return result.next();
                }
            }
        });
    }

    @Override
    public List<Product> search(String keyword) {
        String pattern = "%" + (keyword == null ? "" : keyword.trim().toUpperCase()) + "%";
        return query("SELECT " + COLUMNS + " FROM products "
                + "WHERE UPPER(sku) LIKE ? OR UPPER(name) LIKE ? ORDER BY sku", pattern, pattern);
    }

    @Override
    public List<Product> findByCategoryId(String categoryId) {
        return query("SELECT " + COLUMNS + " FROM products WHERE category_id = ? ORDER BY name, sku",
                categoryId);
    }

    @Override
    public List<Product> findBySupplierId(String supplierId) {
        return query("SELECT " + COLUMNS + " FROM products WHERE supplier_id = ? ORDER BY name, sku",
                supplierId);
    }

    @Override
    public List<Product> findLowStock() {
        return query("SELECT " + COLUMNS + " FROM products "
                + "WHERE quantity <= reorder_level ORDER BY sku");
    }

    @Override
    public List<Product> findAll() {
        return query("SELECT " + COLUMNS + " FROM products ORDER BY sku");
    }

    @Override
    public void save(Product product) {
        database.withConnection(connection -> {
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE products SET sku = ?, name = ?, category_id = ?, supplier_id = ?, "
                            + "unit_price = ?, quantity = ?, reorder_level = ?, unit = ? WHERE id = ?")) {
                bindFields(update, product, 1);
                update.setString(9, product.getId());
                if (update.executeUpdate() > 0) {
                    return null;
                }
            }
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO products (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                insert.setString(1, product.getId());
                bindFields(insert, product, 2);
                insert.executeUpdate();
            }
            return null;
        });
    }

    /** Binds the eight editable columns starting at {@code offset}. */
    private static void bindFields(PreparedStatement statement, Product product, int offset)
            throws SQLException {
        statement.setString(offset, product.getSku());
        statement.setString(offset + 1, product.getName());
        statement.setString(offset + 2, emptyToNull(product.getCategoryId()));
        statement.setString(offset + 3, emptyToNull(product.getSupplierId()));
        statement.setBigDecimal(offset + 4, product.getUnitPrice());
        statement.setInt(offset + 5, product.getQuantity());
        statement.setInt(offset + 6, product.getReorderLevel());
        statement.setString(offset + 7, product.getUnit());
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    @Override
    public void deleteById(String id) {
        JdbcSupport.deleteById(database, "products", id);
    }

    @Override
    public boolean existsById(String id) {
        return JdbcSupport.existsById(database, "products", id);
    }

    @Override
    public int count() {
        return JdbcSupport.count(database, "products");
    }

    private Optional<Product> findOne(String sql, String key) {
        return database.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, key);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next() ? Optional.of(map(result)) : Optional.empty();
                }
            }
        });
    }

    private List<Product> query(String sql, String... parameters) {
        return database.withConnection(connection -> {
            List<Product> products = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (int i = 0; i < parameters.length; i++) {
                    statement.setString(i + 1, parameters[i]);
                }
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        products.add(map(result));
                    }
                }
            }
            return products;
        });
    }

    private static Product map(ResultSet result) throws SQLException {
        BigDecimal unitPrice = result.getBigDecimal("unit_price");
        return new Product(result.getString("id"), result.getString("sku"),
                result.getString("name"), result.getString("category_id"),
                result.getString("supplier_id"), unitPrice, result.getInt("quantity"),
                result.getInt("reorder_level"), result.getString("unit"));
    }
}
