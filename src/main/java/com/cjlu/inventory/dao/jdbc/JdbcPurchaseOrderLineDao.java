package com.cjlu.inventory.dao.jdbc;

import com.cjlu.inventory.dao.ConnectionProvider;
import com.cjlu.inventory.dao.PurchaseOrderLineDao;
import com.cjlu.inventory.model.PurchaseOrderLine;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of {@link PurchaseOrderLineDao}. */
public class JdbcPurchaseOrderLineDao implements PurchaseOrderLineDao {

    private static final String COLUMNS =
            "id, order_id, product_id, quantity, unit_cost, received_quantity";

    private final ConnectionProvider database;

    public JdbcPurchaseOrderLineDao(ConnectionProvider database) {
        this.database = database;
    }

    @Override
    public Optional<PurchaseOrderLine> findById(String id) {
        return database.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT " + COLUMNS + " FROM purchase_order_lines WHERE id = ?")) {
                statement.setString(1, id);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next() ? Optional.of(map(result)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public List<PurchaseOrderLine> findByOrderId(String orderId) {
        return database.withConnection(connection -> {
            List<PurchaseOrderLine> lines = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT " + COLUMNS + " FROM purchase_order_lines WHERE order_id = ? "
                            + "ORDER BY product_id, id")) {
                statement.setString(1, orderId);
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        lines.add(map(result));
                    }
                }
            }
            return lines;
        });
    }

    @Override
    public List<PurchaseOrderLine> findAll() {
        return database.withConnection(connection -> {
            List<PurchaseOrderLine> lines = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT " + COLUMNS + " FROM purchase_order_lines ORDER BY order_id, product_id");
                 ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    lines.add(map(result));
                }
            }
            return lines;
        });
    }

    @Override
    public void save(PurchaseOrderLine line) {
        database.withConnection(connection -> {
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE purchase_order_lines SET order_id = ?, product_id = ?, quantity = ?, "
                            + "unit_cost = ?, received_quantity = ? WHERE id = ?")) {
                bindFields(update, line, 1);
                update.setString(6, line.getId());
                if (update.executeUpdate() > 0) {
                    return null;
                }
            }
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO purchase_order_lines (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?)")) {
                insert.setString(1, line.getId());
                bindFields(insert, line, 2);
                insert.executeUpdate();
            }
            return null;
        });
    }

    private static void bindFields(PreparedStatement statement, PurchaseOrderLine line, int offset)
            throws SQLException {
        statement.setString(offset, line.getOrderId());
        statement.setString(offset + 1, line.getProductId());
        statement.setInt(offset + 2, line.getQuantity());
        statement.setBigDecimal(offset + 3, line.getUnitCost());
        statement.setInt(offset + 4, line.getReceivedQuantity());
    }

    @Override
    public void deleteById(String id) {
        JdbcSupport.deleteById(database, "purchase_order_lines", id);
    }

    @Override
    public boolean existsById(String id) {
        return JdbcSupport.existsById(database, "purchase_order_lines", id);
    }

    @Override
    public int count() {
        return JdbcSupport.count(database, "purchase_order_lines");
    }

    @Override
    public void deleteByOrderId(String orderId) {
        database.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM purchase_order_lines WHERE order_id = ?")) {
                statement.setString(1, orderId);
                statement.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public int countByOrderId(String orderId) {
        return database.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT COUNT(*) FROM purchase_order_lines WHERE order_id = ?")) {
                statement.setString(1, orderId);
                try (ResultSet result = statement.executeQuery()) {
                    result.next();
                    return result.getInt(1);
                }
            }
        });
    }

    private static PurchaseOrderLine map(ResultSet result) throws SQLException {
        return new PurchaseOrderLine(result.getString("id"), result.getString("order_id"),
                result.getString("product_id"), result.getInt("quantity"),
                result.getBigDecimal("unit_cost"), result.getInt("received_quantity"));
    }
}
