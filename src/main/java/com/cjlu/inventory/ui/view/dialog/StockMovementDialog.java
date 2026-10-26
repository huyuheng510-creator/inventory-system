package com.cjlu.inventory.ui.view.dialog;

import com.cjlu.inventory.exception.ValidationException;
import com.cjlu.inventory.model.Product;
import com.cjlu.inventory.ui.util.Theme;
import com.cjlu.inventory.ui.util.UiUtils;

import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;

/**
 * Records a stock-in or stock-out movement. The dialog validates the syntax of
 * the input; the service remains responsible for business rules such as
 * "insufficient stock", whose message is shown as a modal error dialog.
 */
public class StockMovementDialog extends BaseDialog {

    private static final long serialVersionUID = 1L;

    /** Typed movement values collected by the dialog. */
    public record MovementData(int quantity, String note) {
    }

    private final JTextField quantityField = new JTextField(10);
    private final JTextField noteField = new JTextField(30);

    public StockMovementDialog(Window owner, Product product, boolean stockIn,
                               DialogHandler<MovementData> handler) {
        super(owner, stockIn ? "Stock In" : "Stock Out");
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setOpaque(false);

        JPanel summary = new JPanel(new GridBagLayout());
        summary.setOpaque(false);
        summary.setBorder(UiUtils.card().getBorder());
        addSummaryRow(summary, 0, "Product", product.getSku() + " - " + product.getName());
        addSummaryRow(summary, 1, "On Hand", product.getQuantity() + " " + product.getUnit());
        addSummaryRow(summary, 2, "Reorder Level", product.getReorderLevel() + " " + product.getUnit());
        addSummaryRow(summary, 3, "Stock Status", product.getStockStatus().getLabel());

        JPanel form = UiUtils.formPanel();
        UiUtils.addFormRow(form, "Quantity *", quantityField);
        UiUtils.addFormRow(form, "Note (optional)", noteField);

        content.add(summary, BorderLayout.NORTH);
        content.add(form, BorderLayout.CENTER);
        content.add(UiUtils.subtitleLabel(stockIn
                        ? "Stock-in increases the on-hand quantity and appends an audit record."
                        : "Stock-out decreases the on-hand quantity and cannot exceed the available stock."),
                BorderLayout.SOUTH);

        initialize(content, stockIn ? "Record Stock In" : "Record Stock Out",
                () -> execute(stockIn ? "record the stock-in" : "record the stock-out",
                        () -> {
                            int quantity = UiUtils.parseInt(quantityField.getText(), "Quantity");
                            if (quantity <= 0) {
                                throw new ValidationException("Quantity must be a positive whole number.");
                            }
                            handler.save(new MovementData(quantity, noteField.getText()));
                        }));
        centerOnOwner();
    }

    private void addSummaryRow(JPanel panel, int row, String label, String value) {
        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.gridy = row;
        left.anchor = GridBagConstraints.WEST;
        left.insets = new Insets(3, 4, 3, 16);
        javax.swing.JLabel name = new javax.swing.JLabel(label + ":");
        name.setFont(Theme.FONT_SMALL_BOLD);
        name.setForeground(Theme.MUTED);
        panel.add(name, left);

        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1;
        right.gridy = row;
        right.anchor = GridBagConstraints.WEST;
        right.insets = new Insets(3, 0, 3, 4);
        javax.swing.JLabel text = new javax.swing.JLabel(value);
        text.setFont(Theme.FONT_BASE);
        text.setForeground(Theme.TEXT);
        panel.add(text, right);
    }
}
