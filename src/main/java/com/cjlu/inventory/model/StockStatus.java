package com.cjlu.inventory.model;

/**
 * Stock health derived from the product quantity and its reorder level
 * (FR-9). The status is always computed, never stored, so it cannot drift out
 * of sync with the quantity.
 */
public enum StockStatus {

    IN_STOCK("In Stock"),
    LOW_STOCK("Low Stock"),
    OUT_OF_STOCK("Out of Stock");

    private final String label;

    StockStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
