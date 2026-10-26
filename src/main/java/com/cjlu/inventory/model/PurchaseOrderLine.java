package com.cjlu.inventory.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * One product line of a purchase order: a quantity of one product at the
 * negotiated unit cost, plus how much of that quantity has already been
 * received into stock.
 */
public class PurchaseOrderLine extends AbstractEntity {

    private final String orderId;
    private final String productId;
    private final int quantity;
    private final BigDecimal unitCost;
    private int receivedQuantity;

    /** Creates a new order line with a generated id. */
    public PurchaseOrderLine(String orderId, String productId, int quantity, BigDecimal unitCost) {
        super();
        this.orderId = requireText(orderId, "orderId");
        this.productId = requireText(productId, "productId");
        if (quantity <= 0) {
            throw new IllegalArgumentException("order line quantity must be positive");
        }
        this.quantity = quantity;
        this.unitCost = requireCost(unitCost);
        this.receivedQuantity = 0;
    }

    /** Re-creates an order line loaded from the database. */
    public PurchaseOrderLine(String id, String orderId, String productId, int quantity,
                             BigDecimal unitCost, int receivedQuantity) {
        super(id);
        this.orderId = requireText(orderId, "orderId");
        this.productId = requireText(productId, "productId");
        if (quantity <= 0) {
            throw new IllegalArgumentException("order line quantity must be positive");
        }
        this.quantity = quantity;
        this.unitCost = requireCost(unitCost);
        this.receivedQuantity = validReceived(receivedQuantity, quantity);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    private static BigDecimal requireCost(BigDecimal unitCost) {
        Objects.requireNonNull(unitCost, "unitCost");
        if (unitCost.signum() < 0) {
            throw new IllegalArgumentException("unitCost must not be negative");
        }
        return unitCost;
    }

    private static int validReceived(int receivedQuantity, int quantity) {
        if (receivedQuantity < 0 || receivedQuantity > quantity) {
            throw new IllegalArgumentException("receivedQuantity must be between 0 and quantity");
        }
        return receivedQuantity;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public int getReceivedQuantity() {
        return receivedQuantity;
    }

    /** Records a (partial) receipt; cannot exceed the ordered quantity. */
    public void addReceivedQuantity(int quantity) {
        this.receivedQuantity = validReceived(this.receivedQuantity + quantity, this.quantity);
    }

    public int getRemainingQuantity() {
        return quantity - receivedQuantity;
    }

    public BigDecimal getLineTotal() {
        return unitCost.multiply(BigDecimal.valueOf(quantity));
    }

    @Override
    public String toDisplayString() {
        return productId + " x " + quantity + " @ " + unitCost
                + (receivedQuantity > 0 ? " (" + receivedQuantity + " received)" : "");
    }
}
