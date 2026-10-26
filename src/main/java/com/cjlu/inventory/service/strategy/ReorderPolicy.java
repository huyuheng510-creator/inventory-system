package com.cjlu.inventory.service.strategy;

import com.cjlu.inventory.model.Product;
import com.cjlu.inventory.model.StockTransaction;

import java.util.List;

/**
 * Strategy for computing a suggested replenishment quantity (FR-12).
 *
 * <p>The report menu lets the user choose a policy at runtime, so new
 * algorithms can be added without changing {@code ReportService} or the UI
 * (open/closed principle).</p>
 */
public interface ReorderPolicy {

    /** Short human-readable policy name shown in the report. */
    String getName();

    /**
     * @param product             the low-stock product
     * @param recentTransactions  transactions from the recent demand window
     * @return the suggested order quantity (never negative)
     */
    int suggestQuantity(Product product, List<StockTransaction> recentTransactions);
}
