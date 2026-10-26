package com.cjlu.inventory.ui.table;

import com.cjlu.inventory.model.ValuationLine;

import javax.swing.table.AbstractTableModel;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only table model for the inventory valuation report (FR-11). The
 * grand total is shown by the panel below the table.
 */
public class ValuationTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    private static final String[] COLUMNS = {
            "SKU", "Product", "Category", "Qty", "Unit Price", "Line Total"
    };

    private List<ValuationLine> lines = new ArrayList<>();

    public void setLines(List<ValuationLine> lines) {
        this.lines = lines == null ? new ArrayList<>() : new ArrayList<>(lines);
        fireTableDataChanged();
    }

    public List<ValuationLine> getLines() {
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
        ValuationLine line = lines.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> line.getProductSku();
            case 1 -> line.getProductName();
            case 2 -> line.getCategoryName();
            case 3 -> line.getQuantity();
            case 4 -> line.getUnitPrice();
            case 5 -> line.getLineTotal();
            default -> null;
        };
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return switch (columnIndex) {
            case 3 -> Integer.class;
            case 4, 5 -> BigDecimal.class;
            default -> Object.class;
        };
    }
}
