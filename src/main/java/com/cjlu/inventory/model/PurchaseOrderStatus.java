package com.cjlu.inventory.model;

/**
 * Lifecycle of a purchase order in the Project 2 replenishment workflow.
 *
 * <pre>
 * DRAFT -> ORDERED -> PARTIALLY_RECEIVED -> RECEIVED
 *    \         \
 *     +---------+--> CANCELLED
 * </pre>
 */
public enum PurchaseOrderStatus {

    DRAFT("Draft"),
    ORDERED("Ordered"),
    PARTIALLY_RECEIVED("Partially Received"),
    RECEIVED("Received"),
    CANCELLED("Cancelled");

    private final String label;

    PurchaseOrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** True while the order still expects stock (used by the dashboard KPI). */
    public boolean isOpen() {
        return this == DRAFT || this == ORDERED || this == PARTIALLY_RECEIVED;
    }

    /** True when the order can no longer be modified or cancelled. */
    public boolean isFinal() {
        return this == RECEIVED || this == CANCELLED;
    }
}
