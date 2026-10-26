package com.cjlu.inventory.model;

/**
 * Immutable report row containing the suggested replenishment quantity for one
 * low-stock product under a named {@code ReorderPolicy} (FR-12).
 */
public class ReorderSuggestion {

    private final String productSku;
    private final String productName;
    private final String policyName;
    private final int currentQuantity;
    private final int reorderLevel;
    private final int suggestedQuantity;

    public ReorderSuggestion(String productSku, String productName, String policyName,
                             int currentQuantity, int reorderLevel, int suggestedQuantity) {
        if (productSku == null || productSku.isBlank()) {
            throw new IllegalArgumentException("productSku must not be blank");
        }
        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("productName must not be blank");
        }
        if (policyName == null || policyName.isBlank()) {
            throw new IllegalArgumentException("policyName must not be blank");
        }
        if (currentQuantity < 0 || reorderLevel < 0 || suggestedQuantity < 0) {
            throw new IllegalArgumentException("reorder suggestion quantities must not be negative");
        }
        this.productSku = productSku;
        this.productName = productName;
        this.policyName = policyName;
        this.currentQuantity = currentQuantity;
        this.reorderLevel = reorderLevel;
        this.suggestedQuantity = suggestedQuantity;
    }

    public String getProductSku() {
        return productSku;
    }

    public String getProductName() {
        return productName;
    }

    public String getPolicyName() {
        return policyName;
    }

    public int getCurrentQuantity() {
        return currentQuantity;
    }

    public int getReorderLevel() {
        return reorderLevel;
    }

    public int getSuggestedQuantity() {
        return suggestedQuantity;
    }
}
