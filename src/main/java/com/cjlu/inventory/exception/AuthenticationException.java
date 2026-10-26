package com.cjlu.inventory.exception;

/**
 * Raised for failed sign-in attempts and for operations that the current role
 * is not allowed to perform (for example a warehouse staff member trying to
 * change product master data).
 */
public class AuthenticationException extends InventoryException {

    private static final long serialVersionUID = 1L;

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
