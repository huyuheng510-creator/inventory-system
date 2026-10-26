package com.cjlu.inventory.dao;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Supplies JDBC connections to the DAO layer and hides the difference between
 * a stand-alone connection and the connection of an active transaction.
 *
 * <p>DAOs call {@link #withConnection(SqlFunction)} and never close the
 * connection themselves: the implementation closes it after the callback, or
 * reuses the transaction connection when the call is part of a unit of
 * work.</p>
 */
public interface ConnectionProvider {

    /** Runs a query/update callback with a managed connection. */
    <T> T withConnection(SqlFunction<T> work);

    /** Runs a void query/update callback with a managed connection. */
    default void withConnection(SqlConsumer work) {
        withConnection(connection -> {
            work.accept(connection);
            return null;
        });
    }

    @FunctionalInterface
    interface SqlFunction<T> {
        T apply(Connection connection) throws SQLException;
    }

    @FunctionalInterface
    interface SqlConsumer {
        void accept(Connection connection) throws SQLException;
    }
}
