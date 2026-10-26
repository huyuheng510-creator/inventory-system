package com.cjlu.inventory.ui.table;

import com.cjlu.inventory.model.User;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/** Read-only table model for user accounts shown in the administration screen. */
public class UserTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    private static final String[] COLUMNS = {"Username", "Display Name", "Role"};

    private List<User> users = new ArrayList<>();

    public void setUsers(List<User> users) {
        this.users = users == null ? new ArrayList<>() : new ArrayList<>(users);
        fireTableDataChanged();
    }

    public User getUserAt(int modelRow) {
        return users.get(modelRow);
    }

    @Override
    public int getRowCount() {
        return users.size();
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
        User user = users.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> user.getUsername();
            case 1 -> user.getDisplayName();
            case 2 -> user.getRole().getLabel();
            default -> null;
        };
    }
}
