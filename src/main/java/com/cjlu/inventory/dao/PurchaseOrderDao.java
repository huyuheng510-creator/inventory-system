package com.cjlu.inventory.dao;

import com.cjlu.inventory.model.PurchaseOrder;
import com.cjlu.inventory.model.PurchaseOrderStatus;

import java.util.List;

/**
 * Data access for purchase orders (the Project 2 replenishment workflow).
 *
 * <p>Order lines are handled by {@link PurchaseOrderLineDao}; a purchase
 * order is stored as one header row plus N line rows.</p>
 */
public interface PurchaseOrderDao extends Dao<PurchaseOrder> {

    List<PurchaseOrder> findByStatus(PurchaseOrderStatus status);

    List<PurchaseOrder> findBySupplierId(String supplierId);

    /**
     * Produces the next human-readable order number for the given date prefix,
     * for example {@code PO-20261023-004}.
     */
    String nextOrderNumber(String datePrefix);
}
