package com.cjlu.inventory.ui.component;

import com.cjlu.inventory.ui.util.Theme;
import com.cjlu.inventory.ui.util.UiUtils;

import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Component;
import java.math.BigDecimal;

/**
 * Table cell renderer that draws alternating row colours, formats money values
 * as CNY amounts and right-aligns numeric values, giving data tables a
 * readable, modern appearance.
 */
public class StripedTableCellRenderer extends DefaultTableCellRenderer {

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
            setForeground(Theme.TEXT);
        }
        if (value instanceof BigDecimal money) {
            setText(UiUtils.formatMoney(money));
            setHorizontalAlignment(SwingConstants.RIGHT);
        } else if (value instanceof Number) {
            setHorizontalAlignment(SwingConstants.RIGHT);
        } else {
            setHorizontalAlignment(SwingConstants.LEFT);
        }
        setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        return this;
    }
}
