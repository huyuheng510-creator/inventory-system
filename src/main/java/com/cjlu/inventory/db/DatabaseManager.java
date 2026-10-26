package com.cjlu.inventory.db;

import com.cjlu.inventory.dao.ConnectionProvider;
import com.cjlu.inventory.dao.TransactionRunner;
import com.cjlu.inventory.exception.DataAccessException;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Singleton owner of the embedded Apache Derby database (Singleton pattern).
 *
 * <p>One application process has exactly one database configuration: the
 * JDBC URL of the embedded Derby database stored under {@code data/}. The
 * class also acts as the connection provider for every DAO and as the
 * transaction boundary for multi-row use cases.</p>
 *
 * <p>Transactions are tracked with a thread-local connection, so a service can
 * call other services and DAOs inside one transaction without passing a
 * {@code Connection} object through every method signature.</p>
 */
public final class DatabaseManager implements ConnectionProvider, TransactionRunner {

    private static final String EMBEDDED_DRIVER = "org.apache.derby.iapi.jdbc.AutoloadedDriver";
    private static final String LEGACY_EMBEDDED_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";

    private static DatabaseManager instance;

    private final String jdbcUrl;
    private final String shutdownUrl;
    private final ThreadLocal<Connection> transactionConnection = new ThreadLocal<>();

    /** Creates a manager for an explicit JDBC URL (also used by the JUnit tests). */
    public DatabaseManager(String jdbcUrl) {
        Objects.requireNonNull(jdbcUrl, "jdbcUrl");
        this.jdbcUrl = jdbcUrl;
        this.shutdownUrl = jdbcUrl.replace(";create=true", "") + ";shutdown=true";
        loadDriver();
    }

    /**
     * Creates (or connects to) the embedded database directory and makes it the
     * application-wide instance.
     *
     * @param databaseDirectory directory that stores the Derby database files
     */
    public static synchronized DatabaseManager initialize(Path databaseDirectory) {
        Objects.requireNonNull(databaseDirectory, "databaseDirectory");
        String path = databaseDirectory.toAbsolutePath().toString().replace('\\', '/');
        instance = new DatabaseManager("jdbc:derby:" + path + ";create=true");
        return instance;
    }

    /** The application-wide database manager. */
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException(
                    "DatabaseManager.initialize(...) must be called before getInstance().");
        }
        return instance;
    }

    private static void loadDriver() {
        try {
            // Derby 10.16+ registers its JDBC driver under the iapi package; older
            // releases used org.apache.derby.jdbc.EmbeddedDriver.
            Class.forName(EMBEDDED_DRIVER);
        } catch (ClassNotFoundException e) {
            try {
                Class.forName(LEGACY_EMBEDDED_DRIVER);
            } catch (ClassNotFoundException legacyMissing) {
                throw new DataAccessException(
                        "The Apache Derby JDBC driver is missing from the classpath.", legacyMissing);
            }
        }
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    @Override
    public <T> T withConnection(SqlFunction<T> work) {
        Connection transaction = transactionConnection.get();
        if (transaction != null) {
            try {
                return work.apply(transaction);
            } catch (SQLException e) {
                throw new DataAccessException("Database operation failed: " + e.getMessage(), e);
            }
        }
        try (Connection connection = DriverManager.getConnection(jdbcUrl)) {
            return work.apply(connection);
        } catch (SQLException e) {
            throw new DataAccessException("Database operation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void runInTransaction(Runnable work) {
        // Join an already open transaction (nested service calls).
        if (transactionConnection.get() != null) {
            work.run();
            return;
        }
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(jdbcUrl);
            connection.setAutoCommit(false);
            transactionConnection.set(connection);
            try {
                work.run();
                connection.commit();
            } catch (RuntimeException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    e.addSuppressed(rollbackFailure);
                }
                throw e;
            } finally {
                transactionConnection.remove();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Database transaction failed: " + e.getMessage(), e);
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                    connection.close();
                } catch (SQLException ignored) {
                    // The connection is already being discarded.
                }
            }
        }
    }

    /** Flushes and shuts the embedded database down (called when the GUI exits). */
    public void shutdown() {
        if (transactionConnection.get() != null) {
            transactionConnection.remove();
        }
        try {
            DriverManager.getConnection(shutdownUrl);
        } catch (SQLException expected) {
            // Derby always raises SQLState 08006 / XJ015 on a successful shutdown.
        }
    }
}
