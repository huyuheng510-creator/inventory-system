package com.cjlu.inventory.model;

/**
 * Immutable report row that aggregates stock-in, stock-out and adjustment
 * movements per product over a date range (FR-12).
 */
public class MovementLine {

    private final String productSku;
    private final String productName;
    private final int stockInQuantity;
    private final int stockOutQuantity;
    private final int netChange;
    private final int transactionCount;

    public MovementLine(String productSku, String productName, int stockInQuantity,
                        int stockOutQuantity, int netChange, int transactionCount) {
        if (productSku == null || productSku.isBlank()) {
            throw new IllegalArgumentException("productSku must not be blank");
        }
        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("productName must not be blank");
        }
        if (stockInQuantity < 0 || stockOutQuantity < 0 || transactionCount < 0) {
            throw new IllegalArgumentException("movement quantities and counts must not be negative");
        }
        this.productSku = productSku;
        this.productName = productName;
        this.stockInQuantity = stockInQuantity;
        this.stockOutQuantity = stockOutQuantity;
        this.netChange = netChange;
        this.transactionCount = transactionCount;
    }

    public String getProductSku() {
        return productSku;
    }

    public String getProductName() {
        return productName;
    }

    public int getStockInQuantity() {
        return stockInQuantity;
    }

    public int getStockOutQuantity() {
        return stockOutQuantity;
    }

    public int getNetChange() {
        return netChange;
    }

    public int getTransactionCount() {
        return transactionCount;
    }
}
