package com.cjlu.inventory.ui.view.dialog;

import com.cjlu.inventory.exception.InventoryException;

/**
 * Callback used by an input dialog to hand the collected data to its
 * controller. The controller performs the service call; a business failure
 * aborts the dialog so that it stays open with a user-facing error.
 *
 * @param <T> the form data type of the dialog
 */
@FunctionalInterface
public interface DialogHandler<T> {

    /**
     * Persists the data by delegating to a service.
     *
     * @throws InventoryException when the business rules reject the data
     */
    void save(T data) throws InventoryException;
}
