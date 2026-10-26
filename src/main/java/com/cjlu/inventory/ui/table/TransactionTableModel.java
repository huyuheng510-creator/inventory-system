package com.cjlu.inventory.ui.table;

import com.cjlu.inventory.model.StockTransaction;
import com.cjlu.inventory.ui.util.UiUtils;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only table model for the stock transaction history of one product:
 * type, quantity, operator, timestamp and note.
 */
public class TransactionTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    private static final String[] COLUMNS = {"Type", "Quantity", "Operator", "Date / Time", "Note"};

    private List<StockTransaction> transactions = new ArrayList<>();

    public void setTransactions(List<StockTransaction> transactions) {
        this.transactions = transactions == null ? new ArrayList<>() : new ArrayList<>(transactions);
        fireTableDataChanged();
    }

    public StockTransaction getTransactionAt(int modelRow) {
        return transactions.get(modelRow);
    }

    @Override
    public int getRowCount() {
        return transactions.size();
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
        StockTransaction transaction = transactions.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> transaction.getType().getLabel();
            case 1 -> signedQuantity(transaction);
            case 2 -> transaction.getOperatorUsername();
            case 3 -> UiUtils.formatDateTime(transaction.getOccurredAt());
            case 4 -> transaction.getNote();
            default -> null;
        };
    }

    private String signedQuantity(StockTransaction transaction) {
        int amount = transaction.getSignedQuantity();
        return amount > 0 ? "+" + amount : Integer.toString(amount);
    }
}
