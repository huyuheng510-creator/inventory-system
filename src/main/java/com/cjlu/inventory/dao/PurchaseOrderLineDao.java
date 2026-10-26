package com.cjlu.inventory.dao;

import com.cjlu.inventory.model.PurchaseOrderLine;

import java.util.List;

/** Data access for the individual product lines of a purchase order. */
public interface PurchaseOrderLineDao extends Dao<PurchaseOrderLine> {

    List<PurchaseOrderLine> findByOrderId(String orderId);

    void deleteByOrderId(String orderId);

    int countByOrderId(String orderId);
}
