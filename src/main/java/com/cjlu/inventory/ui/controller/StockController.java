package com.cjlu.inventory.ui.controller;

import com.cjlu.inventory.app.ApplicationContext;
import com.cjlu.inventory.exception.InventoryException;
import com.cjlu.inventory.exception.ValidationException;
import com.cjlu.inventory.model.Product;
import com.cjlu.inventory.model.StockTransaction;
import com.cjlu.inventory.ui.util.UiUtils;
import com.cjlu.inventory.ui.view.StockPanel;
import com.cjlu.inventory.ui.view.dialog.StockAdjustmentDialog;
import com.cjlu.inventory.ui.view.dialog.StockMovementDialog;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Stock operations controller: product selection and history loading, the
 * stock-in / stock-out / adjustment use cases and the client-side date range
 * filter of the selected product's audit history.
 */
public final class StockController {

    private final ApplicationContext context;
    private final StockPanel panel;
    private Product currentProduct;
    private List<StockTransaction> fullHistory = List.of();

    public StockController(ApplicationContext context, StockPanel panel) {
        this.context = Objects.requireNonNull(context, "context");
        this.panel = Objects.requireNonNull(panel, "panel");
    }

    /** Initial load: product choices and the history of the first product. */
    public void initialize() {
        reloadProductChoices(null);
    }

    /** Reloads everything while keeping the currently selected product. */
    public void refresh() {
        reloadProductChoices(currentProduct == null ? null : currentProduct.getId());
    }

    /** Re-loads the product combo from the search box. */
    public void refreshProductChoices() {
        reloadProductChoices(currentProduct == null ? null : currentProduct.getId());
    }

    /** Called by the view when the user picks another product. */
    public void onProductSelected(Product product) {
        currentProduct = product;
        if (product == null) {
            panel.setProductInfo(null);
            fullHistory = List.of();
            panel.setHistory(List.of());
            panel.setHistoryCount(0);
            return;
        }
        try {
            panel.setProductInfo(product);
            fullHistory = context.getStockService().historyForProduct(product.getId());
            applyDateFilterInternal();
        } catch (InventoryException failure) {
            UiUtils.handle(panel, "load the transaction history", failure);
        } catch (RuntimeException failure) {
            UiUtils.handle(panel, "load the transaction history", failure);
        }
    }

    /** Applies the optional From/To date filter to the selected product's history. */
    public void applyDateFilter() {
        if (currentProduct == null) {
            UiUtils.info(panel, "Select a product first.");
            return;
        }
        try {
            applyDateFilterInternal();
        } catch (InventoryException failure) {
            UiUtils.handle(panel, "apply the date filter", failure);
        } catch (RuntimeException failure) {
            UiUtils.handle(panel, "apply the date filter", failure);
        }
    }

    /** Clears the date filter fields and re-shows the full history. */
    public void clearDateFilter() {
        panel.clearDateFilterFields();
        applyDateFilter();
    }

    /** Opens the stock-in dialog for the selected product. */
    public void stockIn() {
        Product product = requireSelectedProduct();
        if (product == null) {
            return;
        }
        try {
            StockMovementDialog dialog = new StockMovementDialog(UiUtils.windowOf(panel), product,
                    true, data -> context.getStockService()
                            .stockIn(product.getId(), data.quantity(), data.note()));
            dialog.setVisible(true);
            if (dialog.isSaved()) {
                refresh();
            }
        } catch (RuntimeException failure) {
            UiUtils.handle(panel, "open the stock-in dialog", failure);
        }
    }

    /** Opens the stock-out dialog for the selected product. */
    public void stockOut() {
        Product product = requireSelectedProduct();
        if (product == null) {
            return;
        }
        try {
            StockMovementDialog dialog = new StockMovementDialog(UiUtils.windowOf(panel), product,
                    false, data -> context.getStockService()
                            .stockOut(product.getId(), data.quantity(), data.note()));
            dialog.setVisible(true);
            if (dialog.isSaved()) {
                refresh();
            }
        } catch (RuntimeException failure) {
            UiUtils.handle(panel, "open the stock-out dialog", failure);
        }
    }

    /** Opens the administrator-only stock adjustment dialog. */
    public void adjustStock() {
        if (!context.getAuthService().isAdmin()) {
            UiUtils.warn(panel, "Stock adjustments require administrator privileges.");
            return;
        }
        Product product = requireSelectedProduct();
        if (product == null) {
            return;
        }
        try {
            StockAdjustmentDialog dialog = new StockAdjustmentDialog(UiUtils.windowOf(panel),
                    product, data -> context.getStockService()
                            .adjustStock(product.getId(), data.countedQuantity(), data.reason()));
            dialog.setVisible(true);
            if (dialog.isSaved()) {
                refresh();
            }
        } catch (RuntimeException failure) {
            UiUtils.handle(panel, "open the stock adjustment dialog", failure);
        }
    }

    private void applyDateFilterInternal() throws InventoryException {
        String fromText = panel.getFromDateText();
        String toText = panel.getToDateText();
        boolean fromBlank = fromText == null || fromText.isBlank();
        boolean toBlank = toText == null || toText.isBlank();
        if (fromBlank && toBlank) {
            panel.setHistory(fullHistory);
            panel.setHistoryCount(fullHistory.size());
            return;
        }
        LocalDate from = fromBlank ? null : UiUtils.parseDate(fromText, "From date");
        LocalDate to = toBlank ? null : UiUtils.parseDate(toText, "To date");
        if (from != null && to != null && from.isAfter(to)) {
            throw new ValidationException("The From date must be before the To date.");
        }
        List<StockTransaction> filtered = fullHistory.stream()
                .filter(transaction -> {
                    LocalDate day = transaction.getOccurredAt().toLocalDate();
                    return (from == null || !day.isBefore(from))
                            && (to == null || !day.isAfter(to));
                })
                .toList();
        panel.setHistory(filtered);
        panel.setHistoryCount(filtered.size());
    }

    private void reloadProductChoices(String preferredProductId) {
        try {
            List<Product> products = loadProductChoices();
            panel.setProductChoices(products, preferredProductId);
            onProductSelected(panel.getSelectedProduct());
        } catch (RuntimeException failure) {
            UiUtils.handle(panel, "load the product list", failure);
        }
    }

    private List<Product> loadProductChoices() {
        String searchText = panel.getProductSearchText();
        if (searchText != null && !searchText.isBlank()) {
            return context.getProductService().search(searchText);
        }
        return context.getProductService().listProducts();
    }

    private Product requireSelectedProduct() {
        if (currentProduct == null) {
            UiUtils.info(panel, "Select a product first.");
        }
        return currentProduct;
    }
}
