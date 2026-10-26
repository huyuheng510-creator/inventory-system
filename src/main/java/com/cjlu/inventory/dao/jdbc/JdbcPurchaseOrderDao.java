package com.cjlu.inventory.dao.jdbc;

import com.cjlu.inventory.dao.ConnectionProvider;
import com.cjlu.inventory.dao.PurchaseOrderDao;
import com.cjlu.inventory.model.PurchaseOrder;
import com.cjlu.inventory.model.PurchaseOrderStatus;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of {@link PurchaseOrderDao}. */
public class JdbcPurchaseOrderDao implements PurchaseOrderDao {

    private static final String COLUMNS =
            "id, order_number, supplier_id, status, created_by, created_on, expected_date, received_at, note";

    private final ConnectionProvider database;

    public JdbcPurchaseOrderDao(ConnectionProvider database) {
        this.database = database;
    }

    @Override
    public Optional<PurchaseOrder> findById(String id) {
        return database.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT " + COLUMNS + " FROM purchase_orders WHERE id = ?")) {
                statement.setString(1, id);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next() ? Optional.of(map(result)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public List<PurchaseOrder> findAll() {
        return query("SELECT " + COLUMNS + " FROM purchase_orders ORDER BY created_on DESC, order_number");
    }

    @Override
    public List<PurchaseOrder> findByStatus(PurchaseOrderStatus status) {
        return query("SELECT " + COLUMNS + " FROM purchase_orders WHERE status = ? "
                + "ORDER BY created_on DESC, order_number", status.name());
    }

    @Override
    public List<PurchaseOrder> findBySupplierId(String supplierId) {
        return query("SELECT " + COLUMNS + " FROM purchase_orders WHERE supplier_id = ? "
                + "ORDER BY created_on DESC, order_number", supplierId);
    }

    @Override
    public String nextOrderNumber(String datePrefix) {
        return database.withConnection(connection -> {
            int existing = 0;
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT COUNT(*) FROM purchase_orders WHERE order_number LIKE ?")) {
                statement.setString(1, datePrefix + "%");
                try (ResultSet result = statement.executeQuery()) {
                    result.next();
                    existing = result.getInt(1);
                }
            }
            return datePrefix + String.format("%03d", existing + 1);
        });
    }

    @Override
    public void save(PurchaseOrder order) {
        database.withConnection(connection -> {
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE purchase_orders SET order_number = ?, supplier_id = ?, status = ?, "
                            + "created_by = ?, created_on = ?, expected_date = ?, received_at = ?, "
                            + "note = ? WHERE id = ?")) {
                bindFields(update, order, 1);
                update.setString(9, order.getId());
                if (update.executeUpdate() > 0) {
                    return null;
                }
            }
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO purchase_orders (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                insert.setString(1, order.getId());
                bindFields(insert, order, 2);
                insert.executeUpdate();
            }
            return null;
        });
    }

    private static void bindFields(PreparedStatement statement, PurchaseOrder order, int offset)
            throws SQLException {
        statement.setString(offset, order.getOrderNumber());
        statement.setString(offset + 1, order.getSupplierId());
        statement.setString(offset + 2, order.getStatus().name());
        statement.setString(offset + 3, order.getCreatedBy());
        statement.setTimestamp(offset + 4, Timestamp.valueOf(order.getCreatedOn()));
        statement.setDate(offset + 5, order.getExpectedDate() == null
                ? null : Date.valueOf(order.getExpectedDate()));
        statement.setTimestamp(offset + 6, order.getReceivedAt() == null
                ? null : Timestamp.valueOf(order.getReceivedAt()));
        statement.setString(offset + 7, order.getNote());
    }

    @Override
    public void deleteById(String id) {
        JdbcSupport.deleteById(database, "purchase_orders", id);
    }

    @Override
    public boolean existsById(String id) {
        return JdbcSupport.existsById(database, "purchase_orders", id);
    }

    @Override
    public int count() {
        return JdbcSupport.count(database, "purchase_orders");
    }

    private List<PurchaseOrder> query(String sql, String... parameters) {
        return database.withConnection(connection -> {
            List<PurchaseOrder> orders = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (int i = 0; i < parameters.length; i++) {
                    statement.setString(i + 1, parameters[i]);
                }
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        orders.add(map(result));
                    }
                }
            }
            return orders;
        });
    }

    private static PurchaseOrder map(ResultSet result) throws SQLException {
        Date expected = result.getDate("expected_date");
        Timestamp received = result.getTimestamp("received_at");
        LocalDate expectedDate = expected == null ? null : expected.toLocalDate();
        LocalDateTime receivedAt = received == null ? null : received.toLocalDateTime();
        return new PurchaseOrder(result.getString("id"), result.getString("order_number"),
                result.getString("supplier_id"),
                PurchaseOrderStatus.valueOf(result.getString("status")),
                result.getString("created_by"),
                result.getTimestamp("created_on").toLocalDateTime(),
                expectedDate, receivedAt, result.getString("note"));
    }
}
