package com.cjlu.inventory.exception;

/**
 * Base class for all recoverable, business-level errors in the inventory system.
 *
 * <p>The console layer catches this type, prints {@link #getMessage()} as a
 * single user-friendly line and continues the current menu loop. It is never
 * allowed to reach the top of the application as a stack trace.</p>
 */
public class InventoryException extends Exception {

    private static final long serialVersionUID = 1L;

    public InventoryException(String message) {
        super(message);
    }

    public InventoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
