package com.cjlu.inventory.service;

import com.cjlu.inventory.exception.ValidationException;
import com.cjlu.inventory.model.Product;
import com.cjlu.inventory.model.StockTransaction;
import com.cjlu.inventory.model.TransactionType;

import java.time.LocalDateTime;

/**
 * Factory Method for the three kinds of {@link StockTransaction} (FR-6, FR-7,
 * FR-8).
 *
 * <p>Creating a transaction directly with its constructor is possible but
 * unguarded; this factory centralises the invariants (positive quantities,
 * non-zero signed adjustment, consistent delta and reason) so every service
 * path produces a valid audit entry.</p>
 */
public final class TransactionFactory {

    private TransactionFactory() {
        // static factory
    }

    /** Builds a stock-in transaction with the current unit-price snapshot. */
    public static StockTransaction stockIn(Product product, int quantity,
                                           String operator, String note) throws ValidationException {
        requireProduct(product);
        if (quantity <= 0) {
            throw new ValidationException("Stock-in quantity must be greater than zero.");
        }
        return new StockTransaction(product.getId(), TransactionType.STOCK_IN, quantity,
                product.getUnitPrice(), operator, note, LocalDateTime.now());
    }

    /** Builds a stock-out transaction with the current unit-price snapshot. */
    public static StockTransaction stockOut(Product product, int quantity,
                                            String operator, String note) throws ValidationException {
        requireProduct(product);
        if (quantity <= 0) {
            throw new ValidationException("Stock-out quantity must be greater than zero.");
        }
        return new StockTransaction(product.getId(), TransactionType.STOCK_OUT, quantity,
                product.getUnitPrice(), operator, note, LocalDateTime.now());
    }

    /**
     * Builds an adjustment transaction from the signed difference between the
     * counted and the system quantity.
     */
    public static StockTransaction adjustment(Product product, int signedDelta, int newQuantity,
                                              String operator, String reason)
            throws ValidationException {
        requireProduct(product);
        if (signedDelta == 0) {
            throw new ValidationException(
                    "Physical count matches the system quantity; no adjustment is needed.");
        }
        if (newQuantity < 0) {
            throw new ValidationException("Counted quantity must be zero or a positive whole number.");
        }
        if (product.getQuantity() + signedDelta != newQuantity) {
            throw new ValidationException(
                    "Adjustment delta does not match the physically counted quantity.");
        }
        if (reason == null || reason.isBlank()) {
            throw new ValidationException("A reason is required for a stock adjustment.");
        }
        return new StockTransaction(product.getId(), TransactionType.ADJUSTMENT, signedDelta,
                product.getUnitPrice(), operator, reason.trim(), LocalDateTime.now());
    }

    private static void requireProduct(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("A product is required to create a transaction.");
        }
    }
}
