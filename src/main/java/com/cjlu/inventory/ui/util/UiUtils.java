package com.cjlu.inventory.ui.util;

import com.cjlu.inventory.exception.InventoryException;
import com.cjlu.inventory.exception.ValidationException;
import com.cjlu.inventory.model.Identifiable;
import com.cjlu.inventory.ui.component.StripedTableCellRenderer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.JTableHeader;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Shared Swing helpers: user-facing message dialogs, error handling, value
 * formatting, parsing and the common visual styling of tables, cards and
 * forms.
 *
 * <p>All service failures are presented to the user through this class. No
 * stack trace is ever shown: business failures ({@link InventoryException})
 * display their own message and unexpected failures display a short generic
 * message.</p>
 */
public final class UiUtils {

    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private UiUtils() {
        // utility class
    }

    // ------------------------------------------------------------- messages

    /** Shows an informational message. */
    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "StockFlow", JOptionPane.INFORMATION_MESSAGE);
    }

    /** Shows a warning message. */
    public static void warn(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "StockFlow", JOptionPane.WARNING_MESSAGE);
    }

    /** Shows an error message. */
    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "StockFlow", JOptionPane.ERROR_MESSAGE);
    }

    /** Asks a yes/no question; returns {@code true} only for "Yes". */
    public static boolean confirm(Component parent, String message) {
        int choice = JOptionPane.showConfirmDialog(parent, message, "Please Confirm",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return choice == JOptionPane.YES_OPTION;
    }

    /**
     * Presents a failed action to the user without exposing a stack trace.
     *
     * @param parent       dialog parent (may be {@code null})
     * @param actionPhrase what the user was trying to do, e.g. "save the product"
     * @param failure      the caught failure
     */
    public static void handle(Component parent, String actionPhrase, Throwable failure) {
        if (failure instanceof InventoryException) {
            error(parent, failure.getMessage());
            return;
        }
        String detail = failure == null ? null : failure.getMessage();
        String message = "Could not " + actionPhrase + ".";
        if (detail != null && !detail.isBlank()) {
            message += "\n\n" + detail;
        } else {
            message += "\n\nAn unexpected error occurred. Please try again.";
        }
        error(parent, message);
    }

    // ------------------------------------------------------------ formatting

    /** Formats money as a CNY amount, e.g. {@code ¥1,234.50}. */
    public static String formatMoney(BigDecimal value) {
        BigDecimal safe = value == null ? BigDecimal.ZERO : value;
        return "¥" + MONEY_FORMAT.format(safe);
    }

    /** Compact money label used on bar charts, e.g. {@code ¥12.5K}. */
    public static String formatCompactMoney(BigDecimal value) {
        BigDecimal safe = value == null ? BigDecimal.ZERO : value;
        BigDecimal abs = safe.abs();
        if (abs.compareTo(BigDecimal.valueOf(1_000_000)) >= 0) {
            return "¥" + safe.divide(BigDecimal.valueOf(1_000_000), 1, RoundingMode.HALF_UP) + "M";
        }
        if (abs.compareTo(BigDecimal.valueOf(1_000)) >= 0) {
            return "¥" + safe.divide(BigDecimal.valueOf(1_000), 1, RoundingMode.HALF_UP) + "K";
        }
        return formatMoney(safe);
    }

    /** Formats a date as {@code yyyy-MM-dd}. */
    public static String formatDate(LocalDate value) {
        return value == null ? "-" : DATE_FORMAT.format(value);
    }

    /** Formats a date-time as {@code yyyy-MM-dd HH:mm}. */
    public static String formatDateTime(LocalDateTime value) {
        return value == null ? "-" : DATE_TIME_FORMAT.format(value);
    }

    /** Formats a received/ordered ratio as a whole percentage. */
    public static String formatPercent(int received, int ordered) {
        if (ordered <= 0) {
            return "0%";
        }
        return Math.round(received * 100.0 / ordered) + "%";
    }

    /** Returns {@code value} or a dash when it is blank. */
    public static String orDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    // -------------------------------------------------------------- parsing

    /** Parses a required whole number, throwing a friendly business error. */
    public static int parseInt(String text, String fieldName) throws ValidationException {
        String trimmed = requireText(text, fieldName);
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException notANumber) {
            throw new ValidationException(fieldName + " must be a whole number.");
        }
    }

    /** Parses a required decimal money amount. */
    public static BigDecimal parseDecimal(String text, String fieldName) throws ValidationException {
        String trimmed = requireText(text, fieldName);
        try {
            return new BigDecimal(trimmed);
        } catch (NumberFormatException notANumber) {
            throw new ValidationException(fieldName + " must be a valid number (for example 19.99).");
        }
    }

    /** Parses a required {@code yyyy-MM-dd} date. */
    public static LocalDate parseDate(String text, String fieldName) throws ValidationException {
        String trimmed = requireText(text, fieldName);
        try {
            return LocalDate.parse(trimmed, DATE_FORMAT);
        } catch (DateTimeParseException notADate) {
            throw new ValidationException(fieldName + " must be a valid date in the yyyy-MM-dd format.");
        }
    }

    /** Parses an optional {@code yyyy-MM-dd} date, returning {@code null} when blank. */
    public static LocalDate parseOptionalDate(String text, String fieldName) throws ValidationException {
        if (text == null || text.isBlank()) {
            return null;
        }
        return parseDate(text, fieldName);
    }

    /** Parses an optional date-time range field. */
    public static LocalDateTime parseDateStart(String text, String fieldName) throws ValidationException {
        return parseDate(text, fieldName).atStartOfDay();
    }

    private static String requireText(String text, String fieldName) throws ValidationException {
        if (text == null || text.isBlank()) {
            throw new ValidationException(fieldName + " is required.");
        }
        return text.trim();
    }

    // ------------------------------------------------------------ components

    /** Creates a plain themed button. */
    public static JButton button(String text) {
        JButton button = new JButton(text);
        button.setFont(Theme.FONT_BASE);
        return button;
    }

    /** Creates a prominent button for the primary action of a screen. */
    public static JButton primaryButton(String text) {
        JButton button = button(text);
        button.setFont(Theme.FONT_BOLD);
        button.putClientProperty("JComponent.sizeVariant", "large");
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    /** Creates a small bold button used in toolbars. */
    public static JButton toolbarButton(String text) {
        JButton button = button(text);
        button.setFont(Theme.FONT_SMALL_BOLD);
        button.putClientProperty("JComponent.sizeVariant", "small");
        return button;
    }

    /** Big screen title label. */
    public static JLabel titleLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.FONT_TITLE);
        label.setForeground(Theme.PRIMARY);
        return label;
    }

    /** Section/card title label. */
    public static JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.FONT_SECTION);
        label.setForeground(Theme.TEXT);
        return label;
    }

    /** Small muted description label. */
    public static JLabel subtitleLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.FONT_SMALL);
        label.setForeground(Theme.MUTED);
        return label;
    }

    /** Small muted label. */
    public static JLabel mutedLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.FONT_BASE);
        label.setForeground(Theme.MUTED);
        return label;
    }

    /** Creates a white card panel with padding and a light border. */
    public static JPanel card() {
        JPanel panel = new JPanel();
        panel.setOpaque(true);
        panel.setBackground(Theme.CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        return panel;
    }

    /** Creates a card with a title header and a content component. */
    public static JPanel titledCard(String title, JComponent content) {
        JPanel card = card();
        card.setLayout(new java.awt.BorderLayout(0, 10));
        card.add(sectionTitle(title), java.awt.BorderLayout.NORTH);
        card.add(content, java.awt.BorderLayout.CENTER);
        return card;
    }

    /** Wraps a component in a styled, scrollable viewport. */
    public static JScrollPane scroll(Component view) {
        JScrollPane pane = new JScrollPane(view);
        pane.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1, true));
        pane.getViewport().setBackground(Theme.CARD);
        pane.getVerticalScrollBar().setUnitIncrement(16);
        return pane;
    }

    /** Applies the common table look: row height, striping and headers. */
    public static void styleTable(JTable table) {
        table.setFont(Theme.FONT_BASE);
        table.setRowHeight(27);
        table.setFillsViewportHeight(true);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setGridColor(Theme.BORDER);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateColumnsFromModel(true);
        table.setDefaultRenderer(Object.class, new StripedTableCellRenderer());
        table.setDefaultRenderer(Number.class, new StripedTableCellRenderer());
        JTableHeader header = table.getTableHeader();
        header.setFont(Theme.FONT_SMALL_BOLD);
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(10, 30));
    }

    /** Creates a GridBagLayout form panel for dialog forms. */
    public static JPanel formPanel() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        return form;
    }

    /** Adds one label/field row to a form created by {@link #formPanel()}. */
    public static void addFormRow(JPanel form, String labelText, JComponent field) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = form.getComponentCount() / 2;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.insets = new Insets(6, 0, 6, 12);
        JLabel label = new JLabel(labelText);
        label.setFont(Theme.FONT_BASE);
        label.setForeground(Theme.TEXT);
        form.add(label, labelConstraints);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = labelConstraints.gridy;
        fieldConstraints.weightx = 1.0;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.anchor = GridBagConstraints.WEST;
        fieldConstraints.insets = new Insets(6, 0, 6, 0);
        field.setFont(Theme.FONT_BASE);
        form.add(field, fieldConstraints);
    }

    /** Returns the window that owns a component, or {@code null}. */
    public static Window windowOf(Component component) {
        return component == null ? null : SwingUtilities.getWindowAncestor(component);
    }

    /** Selects the combo item whose id matches {@code id}; does nothing when absent. */
    public static <T extends Identifiable> void selectById(JComboBox<T> combo, String id) {
        if (id == null) {
            return;
        }
        for (int index = 0; index < combo.getItemCount(); index++) {
            T item = combo.getItemAt(index);
            if (item != null && id.equals(item.getId())) {
                combo.setSelectedIndex(index);
                return;
            }
        }
    }

    /** Chooses a readable colour for a stock/status label. */
    public static Color statusColor(String label) {
        if (label == null) {
            return Theme.TEXT;
        }
        return switch (label) {
            case "Out of Stock", "Cancelled" -> Theme.DANGER;
            case "Low Stock", "Partially Received", "Draft" -> Theme.WARNING;
            case "In Stock", "Received" -> Theme.SUCCESS;
            default -> Theme.TEXT;
        };
    }

    /** Small horizontal breathing space. */
    public static Component gap(int width) {
        return javax.swing.Box.createHorizontalStrut(width);
    }
}
