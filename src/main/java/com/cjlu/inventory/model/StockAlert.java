package com.cjlu.inventory.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * An immutable record of one stock-level warning emitted by the Observer
 * (FR-9 extension).
 *
 * <p>Every time {@code StockService} notifies its observers after a stock
 * change, the {@code AlertHistoryService} captures an entry here so that the
 * warnings are visible across the whole session — not just on the status bar
 * until the next event overwrites them.</p>
 */
public final class StockAlert {

    private final String productSku;
    private final String productName;
    private final StockStatus status;
    private final int quantity;
    private final int reorderLevel;
    private final String unit;
    private final LocalDateTime occurredAt;

    public StockAlert(String productSku, String productName, StockStatus status,
                      int quantity, int reorderLevel, String unit, LocalDateTime occurredAt) {
        this.productSku = Objects.requireNonNull(productSku, "productSku");
        this.productName = Objects.requireNonNull(productName, "productName");
        this.status = Objects.requireNonNull(status, "status");
        this.quantity = quantity;
        this.reorderLevel = reorderLevel;
        this.unit = unit == null ? "" : unit;
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
    }

    public String getProductSku()    { return productSku; }
    public String getProductName()   { return productName; }
    public StockStatus getStatus()   { return status; }
    public int getQuantity()         { return quantity; }
    public int getReorderLevel()     { return reorderLevel; }
    public String getUnit()          { return unit; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}
