package com.cjlu.inventory.ui.table;

import com.cjlu.inventory.model.ReorderSuggestion;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/** Read-only table model for the reorder suggestions report (FR-12). */
public class ReorderSuggestionTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    private static final String[] COLUMNS = {
            "SKU", "Product", "Policy", "Current Qty", "Reorder Level", "Suggested Qty"
    };

    private List<ReorderSuggestion> suggestions = new ArrayList<>();

    public void setSuggestions(List<ReorderSuggestion> suggestions) {
        this.suggestions = suggestions == null ? new ArrayList<>() : new ArrayList<>(suggestions);
        fireTableDataChanged();
    }

    public List<ReorderSuggestion> getSuggestions() {
        return List.copyOf(suggestions);
    }

    @Override
    public int getRowCount() {
        return suggestions.size();
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
        ReorderSuggestion suggestion = suggestions.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> suggestion.getProductSku();
            case 1 -> suggestion.getProductName();
            case 2 -> suggestion.getPolicyName();
            case 3 -> suggestion.getCurrentQuantity();
            case 4 -> suggestion.getReorderLevel();
            case 5 -> suggestion.getSuggestedQuantity();
            default -> null;
        };
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnIndex >= 3 ? Integer.class : Object.class;
    }
}
