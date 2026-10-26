package com.cjlu.inventory.exception;

/**
 * Unchecked wrapper for unrecoverable database access failures (connection
 * loss, SQL errors, corrupted schema).
 *
 * <p>Business services keep using the checked {@link InventoryException}
 * hierarchy for expected, user-facing problems. This exception is reserved for
 * infrastructure errors: the GUI catches it at the top level and shows a
 * single friendly error dialog instead of crashing.</p>
 */
public class DataAccessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataAccessException(String message) {
        super(message);
    }
}
