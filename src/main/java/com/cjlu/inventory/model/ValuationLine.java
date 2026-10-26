package com.cjlu.inventory.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Immutable report row for the inventory valuation report (FR-11).
 */
public class ValuationLine {

    private final String productSku;
    private final String productName;
    private final String categoryName;
    private final int quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal lineTotal;

    public ValuationLine(String productSku, String productName, String categoryName,
                         int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
        this.productSku = requireText(productSku, "productSku");
        this.productName = requireText(productName, "productName");
        this.categoryName = requireText(categoryName, "categoryName");
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity must not be negative");
        }
        this.quantity = quantity;
        this.unitPrice = Objects.requireNonNull(unitPrice, "unitPrice");
        this.lineTotal = Objects.requireNonNull(lineTotal, "lineTotal");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    public String getProductSku() {
        return productSku;
    }

    public String getProductName() {
        return productName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }
}
