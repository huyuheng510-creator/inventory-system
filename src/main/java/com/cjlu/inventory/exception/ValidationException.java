package com.cjlu.inventory.exception;

/**
 * Raised when user-supplied data violates a field rule or a business invariant
 * (for example a negative price, a duplicate SKU or an insufficient stock
 * quantity). The message is written for the end user: it names the field and
 * the accepted format or range.
 */
public class ValidationException extends InventoryException {

    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
