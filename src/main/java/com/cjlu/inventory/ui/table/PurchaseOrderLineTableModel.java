package com.cjlu.inventory.ui.table;

import com.cjlu.inventory.model.PurchaseOrderLine;

import javax.swing.table.AbstractTableModel;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only table model for the lines of a purchase order: product, ordered
 * quantity, already received quantity, remaining quantity and money values.
 */
public class PurchaseOrderLineTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    private static final String[] COLUMNS = {
            "SKU", "Product", "Ordered", "Received", "Remaining", "Unit Cost", "Line Total"
    };

    private List<PurchaseOrderLine> lines = new ArrayList<>();
    private Map<String, String> productSkus = new HashMap<>();
    private Map<String, String> productNames = new HashMap<>();

    public void setLines(List<PurchaseOrderLine> lines) {
        this.lines = lines == null ? new ArrayList<>() : new ArrayList<>(lines);
        fireTableDataChanged();
    }

    /** Supplies the product id-to-SKU and id-to-name lookups. */
    public void setProductLookups(Map<String, String> productSkus, Map<String, String> productNames) {
        this.productSkus = productSkus == null ? new HashMap<>() : new HashMap<>(productSkus);
        this.productNames = productNames == null ? new HashMap<>() : new HashMap<>(productNames);
        fireTableDataChanged();
    }

    public PurchaseOrderLine getLineAt(int modelRow) {
        return lines.get(modelRow);
    }

    @Override
    public int getRowCount() {
        return lines.size();
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
        PurchaseOrderLine line = lines.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> productSkus.getOrDefault(line.getProductId(), "-");
            case 1 -> productNames.getOrDefault(line.getProductId(), "Unknown product");
            case 2 -> line.getQuantity();
            case 3 -> line.getReceivedQuantity();
            case 4 -> line.getRemainingQuantity();
            case 5 -> line.getUnitCost();
            case 6 -> line.getLineTotal();
            default -> null;
        };
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return switch (columnIndex) {
            case 2, 3, 4 -> Integer.class;
            case 5, 6 -> BigDecimal.class;
            default -> Object.class;
        };
    }
}
