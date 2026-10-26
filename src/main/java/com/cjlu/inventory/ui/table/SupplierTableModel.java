package com.cjlu.inventory.ui.table;

import com.cjlu.inventory.model.Supplier;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/** Read-only table model for suppliers (administration screen). */
public class SupplierTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    private static final String[] COLUMNS =
            {"Supplier", "Contact", "Phone", "E-mail", "Avg Lead Time"};

    private List<Supplier> suppliers = new ArrayList<>();

    public void setSuppliers(List<Supplier> suppliers) {
        this.suppliers = suppliers == null ? new ArrayList<>() : new ArrayList<>(suppliers);
        fireTableDataChanged();
    }

    public Supplier getSupplierAt(int modelRow) {
        return suppliers.get(modelRow);
    }

    @Override public int getRowCount()    { return suppliers.size(); }
    @Override public int getColumnCount() { return COLUMNS.length; }
    @Override public String getColumnName(int column) { return COLUMNS[column]; }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Supplier supplier = suppliers.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> supplier.getName();
            case 1 -> supplier.getContactName();
            case 2 -> supplier.getPhone();
            case 3 -> supplier.getEmail();
            case 4 -> supplier.leadTimeSummary();
            default -> null;
        };
    }
}
