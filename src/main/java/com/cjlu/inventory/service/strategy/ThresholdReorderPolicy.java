package com.cjlu.inventory.service.strategy;

import com.cjlu.inventory.model.Product;
import com.cjlu.inventory.model.StockTransaction;

import java.util.List;

/**
 * Simple policy: bring the stock back to twice the reorder level.
 *
 * <p>{@code suggested = max(0, reorderLevel * 2 - quantity)}. It needs no
 * history, so it is the safe default for a new catalogue.</p>
 */
public class ThresholdReorderPolicy implements ReorderPolicy {

    @Override
    public String getName() {
        return "Threshold";
    }

    @Override
    public int suggestQuantity(Product product, List<StockTransaction> recentTransactions) {
        if (product == null) {
            return 0;
        }
        int target = product.getReorderLevel() * 2;
        return Math.max(0, target - product.getQuantity());
    }
}
