package com.cjlu.inventory.model;

import com.cjlu.inventory.exception.ValidationException;

import java.math.BigDecimal;

/**
 * The central domain entity: a catalogue product with its current stock level,
 * reorder level and money values.
 *
 * <p>Money is always {@link BigDecimal}; stock quantities are never negative.
 * The class protects its own invariants so that any caller (console, service,
 * future GUI) gets the same protection.</p>
 */
public class Product extends AbstractEntity {

    private String sku;
    private String name;
    private String categoryId;
    private String supplierId;
    private BigDecimal unitPrice;
    private int quantity;
    private int reorderLevel;
    private String unit;

    public Product(String sku, String name, String categoryId, String supplierId,
                   BigDecimal unitPrice, int quantity, int reorderLevel, String unit) {
        super();
        applyFields(sku, name, categoryId, supplierId, unitPrice, quantity, reorderLevel, unit);
    }

    public Product(String id, String sku, String name, String categoryId, String supplierId,
                   BigDecimal unitPrice, int quantity, int reorderLevel, String unit) {
        super(id);
        applyFields(sku, name, categoryId, supplierId, unitPrice, quantity, reorderLevel, unit);
    }

    private void applyFields(String sku, String name, String categoryId, String supplierId,
                             BigDecimal unitPrice, int quantity, int reorderLevel, String unit) {
        this.sku = requireText(sku, "sku");
        this.name = requireText(name, "name");
        this.categoryId = safeId(categoryId);
        this.supplierId = safeId(supplierId);
        this.unitPrice = requireMoney(unitPrice);
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity must not be negative");
        }
        if (reorderLevel < 0) {
            throw new IllegalArgumentException("reorderLevel must not be negative");
        }
        this.quantity = quantity;
        this.reorderLevel = reorderLevel;
        this.unit = requireText(unit, "unit");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    private static String safeId(String value) {
        return value == null ? "" : value.trim();
    }

    private static BigDecimal requireMoney(BigDecimal value) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException("unitPrice must be zero or positive");
        }
        return value;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = requireText(sku, "sku");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = requireText(name, "name");
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = safeId(categoryId);
    }

    public String getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(String supplierId) {
        this.supplierId = safeId(supplierId);
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    /** Sets the unit price; negative values are rejected. */
    public void setUnitPrice(BigDecimal unitPrice) throws ValidationException {
        if (unitPrice == null) {
            throw new ValidationException("Unit price must not be blank.");
        }
        if (unitPrice.signum() < 0) {
            throw new ValidationException("Unit price must be zero or positive.");
        }
        this.unitPrice = unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    /** Sets the on-hand quantity directly; negative values are rejected. */
    public void setQuantity(int quantity) throws ValidationException {
        if (quantity < 0) {
            throw new ValidationException("Quantity must be zero or a positive whole number.");
        }
        this.quantity = quantity;
    }

    public int getReorderLevel() {
        return reorderLevel;
    }

    /** Sets the reorder level; negative values are rejected. */
    public void setReorderLevel(int reorderLevel) throws ValidationException {
        if (reorderLevel < 0) {
            throw new ValidationException("Reorder level must be zero or a positive whole number.");
        }
        this.reorderLevel = reorderLevel;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = requireText(unit, "unit");
    }

    /** Adds stock; the delta must be strictly positive. */
    public void increaseStock(int delta) throws ValidationException {
        if (delta <= 0) {
            throw new ValidationException("Increase quantity must be greater than zero.");
        }
        this.quantity += delta;
    }

    /** Removes stock; the delta must be positive and no larger than the on-hand quantity. */
    public void decreaseStock(int delta) throws ValidationException {
        if (delta <= 0) {
            throw new ValidationException("Decrease quantity must be greater than zero.");
        }
        if (delta > quantity) {
            throw new ValidationException("Insufficient stock: only " + quantity + " " + unit
                    + " of " + sku + " available.");
        }
        this.quantity -= delta;
    }

    /** Stock health derived from the current quantity and reorder level. */
    public StockStatus getStockStatus() {
        if (quantity == 0) {
            return StockStatus.OUT_OF_STOCK;
        }
        if (quantity <= reorderLevel) {
            return StockStatus.LOW_STOCK;
        }
        return StockStatus.IN_STOCK;
    }

    public boolean isLowStock() {
        return getStockStatus() != StockStatus.IN_STOCK;
    }

    /** Unit price multiplied by the current quantity. */
    public BigDecimal getInventoryValue() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    @Override
    public String toDisplayString() {
        return sku + " - " + name;
    }
}
