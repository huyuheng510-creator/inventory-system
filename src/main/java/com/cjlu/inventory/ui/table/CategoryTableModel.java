package com.cjlu.inventory.ui.table;

import com.cjlu.inventory.model.Category;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/** Read-only table model for product categories (administration screen). */
public class CategoryTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    private static final String[] COLUMNS = {"Category", "Description"};

    private List<Category> categories = new ArrayList<>();

    public void setCategories(List<Category> categories) {
        this.categories = categories == null ? new ArrayList<>() : new ArrayList<>(categories);
        fireTableDataChanged();
    }

    public Category getCategoryAt(int modelRow) {
        return categories.get(modelRow);
    }

    @Override
    public int getRowCount() {
        return categories.size();
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
        Category category = categories.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> category.getName();
            case 1 -> category.getDescription();
            default -> null;
        };
    }
}
