package com.cjlu.inventory.service;

import com.cjlu.inventory.model.Product;
import com.cjlu.inventory.model.StockAlert;
import com.cjlu.inventory.model.StockStatus;
import com.cjlu.inventory.service.observer.StockLevelObserver;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * In-session store for stock-level alert events (Observer pattern extension).
 *
 * <p>This service implements {@link StockLevelObserver} and is registered with
 * {@code StockService} at startup. Whenever the service emits a LOW_STOCK or
 * OUT_OF_STOCK notification the alert is appended here. The GUI reads the list
 * to populate the Dashboard "Alerts" panel, making every warning visible for
 * the duration of the session rather than only until the next status-bar
 * message overwrites it.</p>
 *
 * <p>Only warnings (LOW_STOCK and OUT_OF_STOCK) are recorded; a return to
 * IN_STOCK after a restock also adds an "all-clear" entry so the operator can
 * see the full story. The deque is capped at {@value #MAX_ALERTS} entries to
 * avoid unbounded growth in long-running sessions.</p>
 */
public class AlertHistoryService implements StockLevelObserver {

    /** Maximum entries retained; oldest are discarded when this is reached. */
    static final int MAX_ALERTS = 200;

    private final Deque<StockAlert> alerts = new ArrayDeque<>();

    /** Returns all alerts, most recent first. */
    public List<StockAlert> getAlerts() {
        // Deque is in insertion order (oldest first); reverse for the UI.
        List<StockAlert> copy = new ArrayList<>(alerts);
        java.util.Collections.reverse(copy);
        return copy;
    }

    /** Returns only LOW_STOCK and OUT_OF_STOCK alerts, most recent first. */
    public List<StockAlert> getWarningsOnly() {
        return getAlerts().stream()
                .filter(a -> a.getStatus() != StockStatus.IN_STOCK)
                .toList();
    }

    /** Number of alerts recorded this session. */
    public int size() {
        return alerts.size();
    }

    /** Discards all recorded alerts (e.g. after sign-out). */
    public void clear() {
        alerts.clear();
    }

    // ------------------------------------------------ StockLevelObserver

    @Override
    public void onStockLevelChanged(Product product, StockStatus previous, StockStatus current) {
        if (product == null || current == null) {
            return;
        }
        // Record any status change — both the warning and the recovery.
        if (current == previous) {
            return;
        }
        StockAlert alert = new StockAlert(
                product.getSku(),
                product.getName(),
                current,
                product.getQuantity(),
                product.getReorderLevel(),
                product.getUnit(),
                LocalDateTime.now());

        if (alerts.size() >= MAX_ALERTS) {
            alerts.pollFirst();   // discard oldest
        }
        alerts.addLast(alert);
    }
}
