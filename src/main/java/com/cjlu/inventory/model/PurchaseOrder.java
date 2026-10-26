package com.cjlu.inventory.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Purchase order header: the Project 2 replenishment workflow entity.
 *
 * <p>A purchase order groups the quantities that should be bought from one
 * supplier. It starts as a {@link PurchaseOrderStatus#DRAFT}, is submitted by an
 * administrator ({@code ORDERED}) and is then received into stock in one or more
 * deliveries. Receiving the remaining quantities creates ordinary
 * {@link StockTransaction} entries, so purchasing and stock history stay
 * consistent.</p>
 */
public class PurchaseOrder extends AbstractEntity {

    private final String orderNumber;
    private final String supplierId;
    private PurchaseOrderStatus status;
    private final String createdBy;
    private final LocalDateTime createdOn;
    private LocalDate expectedDate;
    private LocalDateTime receivedAt;
    private String note;
    private List<PurchaseOrderLine> lines = new ArrayList<>();

    /** Creates a new draft purchase order with a generated id. */
    public PurchaseOrder(String orderNumber, String supplierId, String createdBy,
                         LocalDate expectedDate, String note) {
        super();
        this.orderNumber = requireText(orderNumber, "orderNumber");
        this.supplierId = requireText(supplierId, "supplierId");
        this.createdBy = requireText(createdBy, "createdBy");
        this.status = PurchaseOrderStatus.DRAFT;
        this.createdOn = LocalDateTime.now();
        this.expectedDate = expectedDate;
        this.note = note == null ? "" : note.trim();
    }

    /** Re-creates a purchase order loaded from the database. */
    public PurchaseOrder(String id, String orderNumber, String supplierId, PurchaseOrderStatus status,
                         String createdBy, LocalDateTime createdOn, LocalDate expectedDate,
                         LocalDateTime receivedAt, String note) {
        super(id);
        this.orderNumber = requireText(orderNumber, "orderNumber");
        this.supplierId = requireText(supplierId, "supplierId");
        this.status = Objects.requireNonNull(status, "status");
        this.createdBy = requireText(createdBy, "createdBy");
        this.createdOn = createdOn == null ? LocalDateTime.now() : createdOn;
        this.expectedDate = expectedDate;
        this.receivedAt = receivedAt;
        this.note = note == null ? "" : note.trim();
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public PurchaseOrderStatus getStatus() {
        return status;
    }

    public void setStatus(PurchaseOrderStatus status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedOn() {
        return createdOn;
    }

    public LocalDate getExpectedDate() {
        return expectedDate;
    }

    public void setExpectedDate(LocalDate expectedDate) {
        this.expectedDate = expectedDate;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note == null ? "" : note.trim();
    }

    public List<PurchaseOrderLine> getLines() {
        return lines;
    }

    public void setLines(List<PurchaseOrderLine> lines) {
        this.lines = lines == null ? new ArrayList<>() : new ArrayList<>(lines);
    }

    /** Sum of the ordered line totals (quantity x unit cost). */
    public BigDecimal getTotalValue() {
        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseOrderLine line : lines) {
            total = total.add(line.getLineTotal());
        }
        return total;
    }

    public int getTotalOrderedQuantity() {
        return lines.stream().mapToInt(PurchaseOrderLine::getQuantity).sum();
    }

    public int getTotalReceivedQuantity() {
        return lines.stream().mapToInt(PurchaseOrderLine::getReceivedQuantity).sum();
    }

    @Override
    public String toDisplayString() {
        return orderNumber + " (" + status.getLabel() + ")";
    }
}
