package com.cjlu.inventory.ui.table;

import com.cjlu.inventory.model.PurchaseOrder;
import com.cjlu.inventory.ui.util.UiUtils;

import javax.swing.table.AbstractTableModel;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only table model for purchase orders: order number, supplier, status,
 * creator, creation time, expected date, total value and received ratio.
 */
public class PurchaseOrderTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    private static final String[] COLUMNS = {
            "Order No.", "Supplier", "Status", "Created By", "Created On",
            "Expected", "Total", "Received"
    };

    private List<PurchaseOrder> orders = new ArrayList<>();
    private Map<String, String> supplierNames = new HashMap<>();

    /** Kept for symmetry with the other models; orders are replaced per filter. */
    public void setOrders(List<PurchaseOrder> orders) {
        this.orders = orders == null ? new ArrayList<>() : new ArrayList<>(orders);
        fireTableDataChanged();
    }

    /** Supplies the supplier id-to-name lookup. */
    public void setSupplierNames(Map<String, String> supplierNames) {
        this.supplierNames = supplierNames == null ? new HashMap<>() : new HashMap<>(supplierNames);
        fireTableDataChanged();
    }

    public PurchaseOrder getOrderAt(int modelRow) {
        return orders.get(modelRow);
    }

    /** Supplier display name for an id, or {@code null} when unknown. */
    public String getSupplierName(String supplierId) {
        return supplierNames.get(supplierId);
    }

    @Override
    public int getRowCount() {
        return orders.size();
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
        PurchaseOrder order = orders.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> order.getOrderNumber();
            case 1 -> supplierNames.getOrDefault(order.getSupplierId(), "Unknown supplier");
            case 2 -> order.getStatus().getLabel();
            case 3 -> order.getCreatedBy();
            case 4 -> UiUtils.formatDateTime(order.getCreatedOn());
            case 5 -> UiUtils.formatDate(order.getExpectedDate());
            case 6 -> order.getTotalValue();
            case 7 -> UiUtils.formatPercent(order.getTotalReceivedQuantity(),
                    order.getTotalOrderedQuantity());
            default -> null;
        };
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnIndex == 6 ? BigDecimal.class : Object.class;
    }
}
