package com.cjlu.inventory.ui.table;

import com.cjlu.inventory.model.MovementLine;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/** Read-only table model for the stock movement report (FR-12). */
public class MovementTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    private static final String[] COLUMNS = {
            "SKU", "Product", "Stock In", "Stock Out", "Net Change", "Transactions"
    };

    private List<MovementLine> lines = new ArrayList<>();

    public void setLines(List<MovementLine> lines) {
        this.lines = lines == null ? new ArrayList<>() : new ArrayList<>(lines);
        fireTableDataChanged();
    }

    public List<MovementLine> getLines() {
        return List.copyOf(lines);
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
        MovementLine line = lines.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> line.getProductSku();
            case 1 -> line.getProductName();
            case 2 -> line.getStockInQuantity();
            case 3 -> line.getStockOutQuantity();
            case 4 -> line.getNetChange();
            case 5 -> line.getTransactionCount();
            default -> null;
        };
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnIndex >= 2 && columnIndex <= 5 ? Integer.class : Object.class;
    }
}
