package com.cjlu.inventory.ui.table;

import com.cjlu.inventory.model.Product;
import com.cjlu.inventory.model.StockStatus;
import com.cjlu.inventory.ui.util.UiUtils;

import javax.swing.table.AbstractTableModel;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only table model for the product catalogue: SKU, name, category,
 * supplier, quantity, reorder level, unit price, stock value and stock status.
 */
public class ProductTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    private static final String[] COLUMNS = {
            "SKU", "Name", "Category", "Supplier", "Qty", "Reorder Level",
            "Unit Price", "Stock Value", "Status"
    };

    private List<Product> products = new ArrayList<>();
    private Map<String, String> categoryNames = new HashMap<>();
    private Map<String, String> supplierNames = new HashMap<>();

    public void setProducts(List<Product> products) {
        this.products = products == null ? new ArrayList<>() : new ArrayList<>(products);
        fireTableDataChanged();
    }

    /** Supplies the id-to-name lookups used by the category and supplier columns. */
    public void setLookups(Map<String, String> categoryNames, Map<String, String> supplierNames) {
        this.categoryNames = categoryNames == null ? new HashMap<>() : new HashMap<>(categoryNames);
        this.supplierNames = supplierNames == null ? new HashMap<>() : new HashMap<>(supplierNames);
        fireTableDataChanged();
    }

    /** Returns the product displayed on a model row. */
    public Product getProductAt(int modelRow) {
        return products.get(modelRow);
    }

    public List<Product> getProducts() {
        return List.copyOf(products);
    }

    @Override
    public int getRowCount() {
        return products.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Product product = products.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> product.getSku();
            case 1 -> product.getName();
            case 2 -> categoryNames.getOrDefault(product.getCategoryId(), "Uncategorised");
            case 3 -> supplierNames.getOrDefault(product.getSupplierId(), "Unknown supplier");
            case 4 -> product.getQuantity();
            case 5 -> product.getReorderLevel();
            case 6 -> product.getUnitPrice();
            case 7 -> product.getInventoryValue();
            case 8 -> product.getStockStatus().getLabel();
            default -> null;
        };
    }

    /** Numeric columns sort naturally with the standard table row sorter. */
    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return switch (columnIndex) {
            case 4, 5 -> Integer.class;
            case 6, 7 -> BigDecimal.class;
            default -> Object.class;
        };
    }

    /** Human-readable stock status of a product (used by the dashboard table). */
    public static String statusLabel(Product product) {
        StockStatus status = product.getStockStatus();
        return status.getLabel() + " (" + product.getQuantity() + " " + product.getUnit() + ")";
    }

    /** Formats the stock status shown in KPI tooltips. */
    public static String describeStock(Product product) {
        return product.getSku() + " " + UiUtils.orDash(product.getName());
    }
}
