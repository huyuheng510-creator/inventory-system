package com.cjlu.inventory.model;

/**
 * The three auditable kinds of stock movement stored in the transaction log
 * (FR-6, FR-7, FR-8, FR-10).
 */
public enum TransactionType {

    STOCK_IN("Stock In"),
    STOCK_OUT("Stock Out"),
    ADJUSTMENT("Adjustment");

    private final String label;

    TransactionType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
