package com.cjlu.inventory.service.observer;

import com.cjlu.inventory.model.Product;
import com.cjlu.inventory.model.StockStatus;

/**
 * Observer notified by {@code StockService} after every committed stock change
 * (FR-9).
 *
 * <p>The stock rules do not know how an alert is displayed; a console observer
 * is registered today and a GUI observer can be added in Project 2 without
 * touching the service.</p>
 */
public interface StockLevelObserver {

    void onStockLevelChanged(Product product, StockStatus previous, StockStatus current);
}
