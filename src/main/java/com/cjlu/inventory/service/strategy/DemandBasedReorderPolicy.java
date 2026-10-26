package com.cjlu.inventory.service.strategy;

import com.cjlu.inventory.model.Product;
import com.cjlu.inventory.model.StockTransaction;
import com.cjlu.inventory.model.TransactionType;

import java.util.List;

/**
 * Demand-driven policy: cover the next seven days of average demand and top the
 * stock back up to the reorder level.
 *
 * <p>{@code suggested = max(0, ceil(averageDailyStockOut * 7) + reorderLevel - quantity)}.
 * Only stock-out movements count as demand; adjustments and stock-ins do not.</p>
 */
public class DemandBasedReorderPolicy implements ReorderPolicy {

    private static final int HISTORY_DAYS = 30;
    private static final int COVER_DAYS = 7;

    @Override
    public String getName() {
        return "7-day demand";
    }

    @Override
    public int suggestQuantity(Product product, List<StockTransaction> recentTransactions) {
        if (product == null) {
            return 0;
        }
        int stockOutQuantity = 0;
        if (recentTransactions != null) {
            for (StockTransaction transaction : recentTransactions) {
                if (transaction.getType() == TransactionType.STOCK_OUT) {
                    stockOutQuantity += transaction.getQuantity();
                }
            }
        }
        double averageDailyDemand = (double) stockOutQuantity / HISTORY_DAYS;
        int forecastForCoverPeriod = (int) Math.ceil(averageDailyDemand * COVER_DAYS);
        int suggested = forecastForCoverPeriod + product.getReorderLevel() - product.getQuantity();
        return Math.max(0, suggested);
    }
}
