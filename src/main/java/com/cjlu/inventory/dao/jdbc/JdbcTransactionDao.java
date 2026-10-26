package com.cjlu.inventory.dao.jdbc;

import com.cjlu.inventory.dao.ConnectionProvider;
import com.cjlu.inventory.dao.TransactionDao;
import com.cjlu.inventory.model.StockTransaction;
import com.cjlu.inventory.model.TransactionType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of {@link TransactionDao}. */
public class JdbcTransactionDao implements TransactionDao {

    private static final String COLUMNS =
            "id, product_id, type, quantity, unit_price, operator_username, note, occurred_at";

    private final ConnectionProvider database;

    public JdbcTransactionDao(ConnectionProvider database) {
        this.database = database;
    }

    @Override
    public Optional<StockTransaction> findById(String id) {
        return database.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT " + COLUMNS + " FROM stock_transactions WHERE id = ?")) {
                statement.setString(1, id);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next() ? Optional.of(map(result)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public List<StockTransaction> findAll() {
        return query("SELECT " + COLUMNS + " FROM stock_transactions ORDER BY occurred_at, id");
    }

    @Override
    public List<StockTransaction> findByProductId(String productId) {
        return query("SELECT " + COLUMNS + " FROM stock_transactions "
                + "WHERE product_id = ? ORDER BY occurred_at, id", productId);
    }

    @Override
    public List<StockTransaction> findBetween(LocalDateTime fromInclusive, LocalDateTime toExclusive) {
        return database.withConnection(connection -> {
            List<StockTransaction> transactions = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT " + COLUMNS + " FROM stock_transactions "
                            + "WHERE occurred_at >= ? AND occurred_at < ? ORDER BY occurred_at, id")) {
                statement.setTimestamp(1, Timestamp.valueOf(fromInclusive));
                statement.setTimestamp(2, Timestamp.valueOf(toExclusive));
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        transactions.add(map(result));
                    }
                }
            }
            return transactions;
        });
    }

    @Override
    public List<StockTransaction> findRecent(int limit) {
        return database.withConnection(connection -> {
            List<StockTransaction> transactions = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT " + COLUMNS + " FROM stock_transactions "
                            + "ORDER BY occurred_at DESC, id FETCH FIRST ? ROWS ONLY")) {
                statement.setInt(1, limit);
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        transactions.add(map(result));
                    }
                }
            }
            return transactions;
        });
    }

    @Override
    public void save(StockTransaction transaction) {
        database.withConnection(connection -> {
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE stock_transactions SET product_id = ?, type = ?, quantity = ?, "
                            + "unit_price = ?, operator_username = ?, note = ?, occurred_at = ? WHERE id = ?")) {
                bindFields(update, transaction, 1);
                update.setString(8, transaction.getId());
                if (update.executeUpdate() > 0) {
                    return null;
                }
            }
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO stock_transactions (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                insert.setString(1, transaction.getId());
                bindFields(insert, transaction, 2);
                insert.executeUpdate();
            }
            return null;
        });
    }

    /** Binds the seven editable columns starting at {@code offset}. */
    private static void bindFields(PreparedStatement statement, StockTransaction transaction,
                                   int offset) throws SQLException {
        statement.setString(offset, transaction.getProductId());
        statement.setString(offset + 1, transaction.getType().name());
        statement.setInt(offset + 2, transaction.getQuantity());
        statement.setBigDecimal(offset + 3, transaction.getUnitPrice());
        statement.setString(offset + 4, transaction.getOperatorUsername());
        statement.setString(offset + 5, transaction.getNote());
        statement.setTimestamp(offset + 6, Timestamp.valueOf(transaction.getOccurredAt()));
    }

    private List<StockTransaction> query(String sql, String... parameters) {
        return database.withConnection(connection -> {
            List<StockTransaction> transactions = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (int i = 0; i < parameters.length; i++) {
                    statement.setString(i + 1, parameters[i]);
                }
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        transactions.add(map(result));
                    }
                }
            }
            return transactions;
        });
    }

    @Override
    public void deleteById(String id) {
        JdbcSupport.deleteById(database, "stock_transactions", id);
    }

    @Override
    public boolean existsById(String id) {
        return JdbcSupport.existsById(database, "stock_transactions", id);
    }

    @Override
    public int count() {
        return JdbcSupport.count(database, "stock_transactions");
    }

    private static StockTransaction map(ResultSet result) throws SQLException {
        return new StockTransaction(result.getString("id"), result.getString("product_id"),
                TransactionType.valueOf(result.getString("type")), result.getInt("quantity"),
                result.getBigDecimal("unit_price"), result.getString("operator_username"),
                result.getString("note"), result.getTimestamp("occurred_at").toLocalDateTime());
    }
}
