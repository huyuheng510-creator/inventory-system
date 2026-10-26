package com.cjlu.inventory.ui.view.dialog;

import com.cjlu.inventory.exception.ValidationException;
import com.cjlu.inventory.model.Product;
import com.cjlu.inventory.ui.util.UiUtils;

import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Window;

/**
 * Administrator-only stock adjustment: records the physically counted
 * quantity and stores the signed difference with a mandatory reason.
 */
public class StockAdjustmentDialog extends BaseDialog {

    private static final long serialVersionUID = 1L;

    /** Typed adjustment values collected by the dialog. */
    public record AdjustmentData(int countedQuantity, String reason) {
    }

    private final JTextField countedQuantityField = new JTextField(10);
    private final JTextField reasonField = new JTextField(30);

    public StockAdjustmentDialog(Window owner, Product product,
                                 DialogHandler<AdjustmentData> handler) {
        super(owner, "Stock Adjustment");
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setOpaque(false);

        JPanel info = UiUtils.formPanel();
        UiUtils.addFormRow(info, "Product", UiUtils.mutedLabel(
                product.getSku() + " - " + product.getName()));
        UiUtils.addFormRow(info, "System Quantity", UiUtils.mutedLabel(
                product.getQuantity() + " " + product.getUnit()));
        UiUtils.addFormRow(info, "Reorder Level", UiUtils.mutedLabel(
                product.getReorderLevel() + " " + product.getUnit()));

        JPanel form = UiUtils.formPanel();
        UiUtils.addFormRow(form, "Counted Quantity *", countedQuantityField);
        UiUtils.addFormRow(form, "Reason *", reasonField);

        JPanel forms = new JPanel(new BorderLayout(0, 8));
        forms.setOpaque(false);
        forms.add(info, BorderLayout.NORTH);
        forms.add(form, BorderLayout.CENTER);

        content.add(forms, BorderLayout.CENTER);
        content.add(UiUtils.subtitleLabel(
                "The adjustment is stored as the difference between the counted and system quantity."),
                BorderLayout.SOUTH);

        initialize(content, "Record Adjustment", () -> execute("record the stock adjustment", () -> {
            int countedQuantity = UiUtils.parseInt(countedQuantityField.getText(), "Counted quantity");
            if (countedQuantity < 0) {
                throw new ValidationException("Counted quantity must be zero or a positive whole number.");
            }
            handler.save(new AdjustmentData(countedQuantity, reasonField.getText()));
        }));
        centerOnOwner();
    }
}
