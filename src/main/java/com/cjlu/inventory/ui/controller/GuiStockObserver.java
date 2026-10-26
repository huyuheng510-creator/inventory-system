package com.cjlu.inventory.ui.controller;

import com.cjlu.inventory.model.Product;
import com.cjlu.inventory.model.StockStatus;
import com.cjlu.inventory.service.observer.StockLevelObserver;
import com.cjlu.inventory.ui.util.Theme;

import javax.swing.SwingUtilities;
import java.awt.Color;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * GUI implementation of the Observer pattern: {@code StockService} notifies
 * this observer after every committed stock change.
 *
 * <p>The notification itself only formats a status message; the actual status
 * bar update and the dashboard/product-table refresh are pushed onto the
 * Event Dispatch Thread with {@link SwingUtilities#invokeLater(Runnable)}, so
 * no database work happens inside the service transaction.</p>
 */
public final class GuiStockObserver implements StockLevelObserver {

    private final BiConsumer<String, Color> statusSink;
    private final Runnable refreshAction;
    private volatile boolean active = true;

    /**
     * @param statusSink    receives the status-bar message and its colour
     * @param refreshAction refreshes the dashboard and product table
     */
    public GuiStockObserver(BiConsumer<String, Color> statusSink, Runnable refreshAction) {
        this.statusSink = Objects.requireNonNull(statusSink, "statusSink");
        this.refreshAction = Objects.requireNonNull(refreshAction, "refreshAction");
    }

    /** Stops reacting to stock changes, e.g. after logout or exit. */
    public void deactivate() {
        active = false;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public void onStockLevelChanged(Product product, StockStatus previous, StockStatus current) {
        if (!active || product == null || current == null) {
            return;
        }
        String message;
        Color color;
        if (current == StockStatus.OUT_OF_STOCK) {
            message = "Out of stock: " + product.getSku() + " - " + product.getName()
                    + " (0 " + product.getUnit() + ", reorder level "
                    + product.getReorderLevel() + ").";
            color = Theme.DANGER;
        } else if (current == StockStatus.LOW_STOCK) {
            message = "Low stock: " + product.getSku() + " - " + product.getName()
                    + " (" + product.getQuantity() + " " + product.getUnit()
                    + " left, reorder level " + product.getReorderLevel() + ").";
            color = Theme.WARNING;
        } else {
            message = "Stock updated: " + product.getSku() + " - " + product.getName()
                    + " now " + product.getQuantity() + " " + product.getUnit() + ".";
            color = Theme.SUCCESS;
        }
        SwingUtilities.invokeLater(() -> {
            if (!active) {
                return;
            }
            statusSink.accept(message, color);
            refreshAction.run();
        });
    }
}
