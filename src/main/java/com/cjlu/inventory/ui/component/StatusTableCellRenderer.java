package com.cjlu.inventory.ui.component;

import com.cjlu.inventory.ui.util.Theme;
import com.cjlu.inventory.ui.util.UiUtils;

import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Component;

/**
 * Table cell renderer for status columns: keeps the alternating row colours
 * but paints "In Stock" green, "Low Stock" amber, "Out of Stock" red and the
 * purchase-order statuses in matching colours.
 */
public class StatusTableCellRenderer extends DefaultTableCellRenderer {

    private static final long serialVersionUID = 1L;

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (isSelected) {
            setBackground(table.getSelectionBackground());
            setForeground(table.getSelectionForeground());
        } else {
            setBackground(row % 2 == 0 ? Theme.CARD : Theme.TABLE_ROW_ALT);
            setForeground(UiUtils.statusColor(value == null ? null : value.toString()));
        }
        setFont(Theme.FONT_SMALL_BOLD);
        setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        return this;
    }
}
