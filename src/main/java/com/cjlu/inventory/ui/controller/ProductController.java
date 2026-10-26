package com.cjlu.inventory.ui.controller;

import com.cjlu.inventory.app.ApplicationContext;
import com.cjlu.inventory.model.Category;
import com.cjlu.inventory.model.Product;
import com.cjlu.inventory.model.Supplier;
import com.cjlu.inventory.service.SortField;
import com.cjlu.inventory.ui.util.UiUtils;
import com.cjlu.inventory.ui.view.ProductPanel;
import com.cjlu.inventory.ui.view.dialog.ProductDialog;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Product catalogue controller: loads the filtered/sorted product list,
 * performs create/update/delete through {@code ProductService} and keeps the
 * view's lookups (category and supplier names) up to date.
 *
 * <p>Mutating operations are guarded by the view (buttons are hidden for
 * warehouse staff) and by the service (defence in depth).</p>
 */
public final class ProductController {

    private final ApplicationContext context;
    private final ProductPanel panel;
    private List<Category> categories = List.of();
    private List<Supplier> suppliers = List.of();
    private Map<String, String> categoryNames = new HashMap<>();
    private Map<String, String> supplierNames = new HashMap<>();

    public ProductController(ApplicationContext context, ProductPanel panel) {
        this.context = Objects.requireNonNull(context, "context");
        this.panel = Objects.requireNonNull(panel, "panel");
    }

    /** Initial load after the panel is constructed. */
    public void initialize() {
        refresh();
    }

    /** Reloads the lookups and the product table, keeping the current filters. */
    public void refresh() {
        try {
            reloadLookups();
            List<Product> products = loadFilteredProducts();
            panel.setProducts(products);
            panel.setCountLabel(products.size());
        } catch (RuntimeException failure) {
            UiUtils.handle(panel, "load the product list", failure);
        }
    }

    /** Applies the search box, category filter and sort choice. */
    public void applyFilters() {
        refresh();
    }

    /** Opens the empty product form (administrator only). */
    public void showAddDialog() {
        if (!requireAdmin()) {
            return;
        }
        try {
            reloadLookups();
            if (categories.isEmpty() || suppliers.isEmpty()) {
                UiUtils.info(panel, "Create at least one category and one supplier in the "
                        + "Administration tab before adding products.");
                return;
            }
            ProductDialog dialog = new ProductDialog(UiUtils.windowOf(panel), false, null,
                    categories, suppliers, data -> context.getProductService().createProduct(
                            data.sku(), data.name(), data.categoryId(), data.supplierId(),
                            data.unitPrice(), data.quantity(), data.reorderLevel(), data.unit()));
            dialog.setVisible(true);
            if (dialog.isSaved()) {
                refresh();
            }
        } catch (RuntimeException failure) {
            UiUtils.handle(panel, "open the new-product dialog", failure);
        }
    }

    /** Opens the edit form for the selected product (administrator only). */
    public void showEditDialog() {
        if (!requireAdmin()) {
            return;
        }
        Product selected = requireSelectedProduct();
        if (selected == null) {
            return;
        }
        try {
            reloadLookups();
            ProductDialog dialog = new ProductDialog(UiUtils.windowOf(panel), true, selected,
                    categories, suppliers, data -> context.getProductService().updateProduct(
                            selected.getId(), data.name(), data.categoryId(), data.supplierId(),
                            data.unitPrice(), data.reorderLevel(), data.unit()));
            dialog.setVisible(true);
            if (dialog.isSaved()) {
                refresh();
            }
        } catch (RuntimeException failure) {
            UiUtils.handle(panel, "open the edit-product dialog", failure);
        }
    }

    /** Deletes the selected product after confirmation (administrator only). */
    public void deleteSelected() {
        if (!requireAdmin()) {
            return;
        }
        Product selected = requireSelectedProduct();
        if (selected == null) {
            return;
        }
        if (!UiUtils.confirm(panel, "Delete product " + selected.getSku() + " - "
                + selected.getName() + "?\n\nThis cannot be undone.")) {
            return;
        }
        try {
            context.getProductService().deleteProduct(selected.getId());
            refresh();
        } catch (com.cjlu.inventory.exception.InventoryException failure) {
            UiUtils.handle(panel, "delete the product", failure);
        } catch (RuntimeException failure) {
            UiUtils.handle(panel, "delete the product", failure);
        }
    }

    /** Shows all details of the selected product. */
    public void showDetails() {
        Product selected = requireSelectedProduct();
        if (selected == null) {
            return;
        }
        StringBuilder text = new StringBuilder();
        text.append("SKU: ").append(selected.getSku()).append('\n');
        text.append("Name: ").append(selected.getName()).append('\n');
        text.append("Category: ").append(categoryNames.getOrDefault(
                selected.getCategoryId(), "Uncategorised")).append('\n');
        text.append("Supplier: ").append(supplierNames.getOrDefault(
                selected.getSupplierId(), "Unknown supplier")).append('\n');
        text.append("Quantity: ").append(selected.getQuantity()).append(' ')
                .append(selected.getUnit()).append('\n');
        text.append("Reorder Level: ").append(selected.getReorderLevel()).append(' ')
                .append(selected.getUnit()).append('\n');
        text.append("Unit Price: ").append(UiUtils.formatMoney(selected.getUnitPrice())).append('\n');
        text.append("Stock Value: ").append(UiUtils.formatMoney(selected.getInventoryValue()))
                .append('\n');
        text.append("Stock Status: ").append(selected.getStockStatus().getLabel()).append('\n');
        text.append("Unit: ").append(selected.getUnit());
        UiUtils.info(panel, text.toString());
    }

    private void reloadLookups() {
        categories = context.getCatalogService().listCategories();
        suppliers = context.getCatalogService().listSuppliers();
        Map<String, String> newCategoryNames = new HashMap<>();
        for (Category category : categories) {
            newCategoryNames.put(category.getId(), category.getName());
        }
        Map<String, String> newSupplierNames = new HashMap<>();
        for (Supplier supplier : suppliers) {
            newSupplierNames.put(supplier.getId(), supplier.getName());
        }
        categoryNames = newCategoryNames;
        supplierNames = newSupplierNames;
        panel.setCategories(categories);
        panel.setLookups(categoryNames, supplierNames);
    }

    private List<Product> loadFilteredProducts() {
        List<Product> products;
        String searchText = panel.getSearchText();
        if (searchText != null && !searchText.isBlank()) {
            products = new ArrayList<>(context.getProductService().search(searchText));
        } else {
            products = new ArrayList<>(context.getProductService().listProducts());
        }
        String categoryId = panel.getSelectedCategoryId();
        if (categoryId != null && !categoryId.isBlank()) {
            products.removeIf(product -> !categoryId.equals(product.getCategoryId()));
        }
        SortField sortField = panel.getSelectedSortField();
        if (sortField != null) {
            Comparator<Product> comparator = switch (sortField) {
                case NAME -> Comparator.comparing(Product::getName,
                        String.CASE_INSENSITIVE_ORDER);
                case QUANTITY -> Comparator.comparingInt(Product::getQuantity);
                case VALUE -> Comparator.comparing(Product::getInventoryValue);
            };
            products.sort(comparator.thenComparing(Product::getSku));
        }
        return products;
    }

    private boolean requireAdmin() {
        if (!context.getAuthService().isAdmin()) {
            UiUtils.warn(panel, "This action requires administrator privileges.");
            return false;
        }
        return true;
    }

    private Product requireSelectedProduct() {
        Product selected = panel.getSelectedProduct();
        if (selected == null) {
            UiUtils.info(panel, "Select a product in the table first.");
        }
        return selected;
    }
}
