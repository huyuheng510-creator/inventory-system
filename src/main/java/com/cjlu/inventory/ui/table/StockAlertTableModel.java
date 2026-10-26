package com.cjlu.inventory.ui.table;

import com.cjlu.inventory.model.StockAlert;
import com.cjlu.inventory.model.StockStatus;
import com.cjlu.inventory.ui.util.UiUtils;

import javax.swing.table.AbstractTableModel;
import java.util.List;

/**
 * Table model for the session-level stock-alert history shown on the Dashboard.
 */
public class StockAlertTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;
    private static final String[] COLUMNS = {
            "Time", "SKU", "Product", "Status", "Qty", "Reorder"
    };

    private List<StockAlert> alerts = List.of();

    public void setAlerts(List<StockAlert> alerts) {
        this.alerts = List.copyOf(alerts);
        fireTableDataChanged();
    }

    @Override public int getRowCount()    { return alerts.size(); }
    @Override public int getColumnCount() { return COLUMNS.length; }
    @Override public String getColumnName(int col) { return COLUMNS[col]; }

    @Override
    public Class<?> getColumnClass(int col) {
        return String.class;
    }

    @Override
    public Object getValueAt(int row, int col) {
        StockAlert a = alerts.get(row);
        return switch (col) {
            case 0 -> UiUtils.formatDateTime(a.getOccurredAt());
            case 1 -> a.getProductSku();
            case 2 -> a.getProductName();
            case 3 -> a.getStatus().getLabel();
            case 4 -> a.getQuantity() + " " + a.getUnit();
            case 5 -> String.valueOf(a.getReorderLevel());
            default -> "";
        };
    }

    /** Returns the alert at the given model row, or {@code null} if out of range. */
    public StockAlert getAlertAt(int row) {
        return (row >= 0 && row < alerts.size()) ? alerts.get(row) : null;
    }
}
