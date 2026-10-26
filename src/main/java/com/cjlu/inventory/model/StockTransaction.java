package com.cjlu.inventory.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * One immutable entry of the auditable stock transaction log (FR-6, FR-7,
 * FR-8, FR-10).
 *
 * <p>{@code STOCK_IN} and {@code STOCK_OUT} store the quantity as a positive
 * magnitude and derive the direction from the type. An {@code ADJUSTMENT}
 * stores the signed difference between the physically counted quantity and the
 * system quantity (FR-8), so a negative adjustment means stock was found
 * missing.</p>
 */
public class StockTransaction extends AbstractEntity {

    private final String productId;
    private final TransactionType type;
    private final int quantity;
    private final BigDecimal unitPrice;
    private final String operatorUsername;
    private final String note;
    private final LocalDateTime occurredAt;

    public StockTransaction(String productId, TransactionType type, int quantity,
                            BigDecimal unitPrice, String operatorUsername, String note) {
        this(productId, type, quantity, unitPrice, operatorUsername, note, LocalDateTime.now());
    }

    public StockTransaction(String productId, TransactionType type, int quantity, BigDecimal unitPrice,
                            String operatorUsername, String note, LocalDateTime occurredAt) {
        super();
        this.productId = requireText(productId, "productId");
        this.type = Objects.requireNonNull(type, "type");
        if (quantity == 0) {
            throw new IllegalArgumentException("transaction quantity must not be zero");
        }
        this.quantity = quantity;
        this.unitPrice = Objects.requireNonNull(unitPrice, "unitPrice");
        if (unitPrice.signum() < 0) {
            throw new IllegalArgumentException("transaction unit price must not be negative");
        }
        this.operatorUsername = requireText(operatorUsername, "operatorUsername");
        this.note = note == null ? "" : note.trim();
        this.occurredAt = occurredAt == null ? LocalDateTime.now() : occurredAt;
    }

    public StockTransaction(String id, String productId, TransactionType type, int quantity,
                            BigDecimal unitPrice, String operatorUsername, String note,
                            LocalDateTime occurredAt) {
        super(id);
        this.productId = requireText(productId, "productId");
        this.type = Objects.requireNonNull(type, "type");
        if (quantity == 0) {
            throw new IllegalArgumentException("transaction quantity must not be zero");
        }
        this.quantity = quantity;
        this.unitPrice = Objects.requireNonNull(unitPrice, "unitPrice");
        if (unitPrice.signum() < 0) {
            throw new IllegalArgumentException("transaction unit price must not be negative");
        }
        this.operatorUsername = requireText(operatorUsername, "operatorUsername");
        this.note = note == null ? "" : note.trim();
        this.occurredAt = occurredAt == null ? LocalDateTime.now() : occurredAt;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    public String getProductId() {
        return productId;
    }

    public TransactionType getType() {
        return type;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public String getOperatorUsername() {
        return operatorUsername;
    }

    public String getNote() {
        return note;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    /** Signed effect on the on-hand quantity: positive for stock-in, negative for stock-out. */
    public int getSignedQuantity() {
        return switch (type) {
            case STOCK_IN -> quantity;
            case STOCK_OUT -> -quantity;
            case ADJUSTMENT -> quantity;
        };
    }

    /** Absolute monetary value of the movement at the snapshot unit price. */
    public BigDecimal getTotalValue() {
        return unitPrice.multiply(BigDecimal.valueOf(Math.abs((long) quantity)));
    }

    @Override
    public String toDisplayString() {
        return type.getLabel() + " " + Math.abs(quantity) + " @ " + unitPrice
                + " by " + operatorUsername + " on " + occurredAt;
    }
}
