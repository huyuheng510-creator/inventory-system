package com.cjlu.inventory.exception;

/**
 * Raised when an entity is looked up by id or by a natural key (SKU, username,
 * category name, ...) and does not exist.
 */
public class EntityNotFoundException extends InventoryException {

    private static final long serialVersionUID = 1L;

    public EntityNotFoundException(String message) {
        super(message);
    }

    public EntityNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
