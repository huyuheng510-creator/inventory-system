package com.cjlu.inventory.ui.view.dialog;

import com.cjlu.inventory.exception.ValidationException;
import com.cjlu.inventory.model.Product;
import com.cjlu.inventory.model.Supplier;
import com.cjlu.inventory.service.PurchaseOrderService;
import com.cjlu.inventory.service.PurchaseOrderService.OrderLineDraft;
import com.cjlu.inventory.ui.component.DisplayRenderer;
import com.cjlu.inventory.ui.util.Theme;
import com.cjlu.inventory.ui.util.UiUtils;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates a draft purchase order with a dynamic list of product lines.
 *
 * <p>Each line combines a product selector, an order quantity and the
 * negotiated unit cost; lines can be added and removed until at least one
 * valid line remains. The completed {@link DraftData} is handed to the
 * controller, which calls {@code PurchaseOrderService.createDraft}.</p>
 */
public class PurchaseOrderDialog extends BaseDialog {

    private static final long serialVersionUID = 1L;

    /** Typed draft values collected by the dialog. */
    public record DraftData(String supplierId, LocalDate expectedDate, String note,
                            List<OrderLineDraft> lines) {
    }

    private final List<Product> products;
    private final JComboBox<Supplier> supplierCombo;
    private final JTextField expectedDateField = new JTextField(12);
    private final JTextField noteField = new JTextField(32);
    private final JPanel linesPanel = new JPanel();
    private final List<LineRow> lineRows = new ArrayList<>();

    public PurchaseOrderDialog(Window owner, List<Supplier> suppliers, List<Product> products,
                               DialogHandler<DraftData> handler) {
        super(owner, "New Purchase Order Draft");
        this.products = List.copyOf(products);

        supplierCombo = new JComboBox<>(suppliers.toArray(new Supplier[0]));
        supplierCombo.setRenderer(new DisplayRenderer<Supplier>(
                supplier -> supplier == null ? "" : supplier.getName()));
        if (supplierCombo.getItemCount() > 0) {
            supplierCombo.setSelectedIndex(0);
        }
        expectedDateField.setText(UiUtils.formatDate(LocalDate.now().plusDays(7)));

        JPanel form = UiUtils.formPanel();
        UiUtils.addFormRow(form, "Supplier *", supplierCombo);
        UiUtils.addFormRow(form, "Expected Delivery", expectedDateField);
        UiUtils.addFormRow(form, "Note (optional)", noteField);

        linesPanel.setLayout(new javax.swing.BoxLayout(linesPanel, javax.swing.BoxLayout.Y_AXIS));
        linesPanel.setBackground(Theme.CARD);
        JScrollPane scroll = UiUtils.scroll(linesPanel);
        scroll.setPreferredSize(new Dimension(700, 220));

        JPanel linesCard = new JPanel(new BorderLayout(0, 8));
        linesCard.setOpaque(false);
        linesCard.add(buildLineHeader(), BorderLayout.NORTH);
        linesCard.add(scroll, BorderLayout.CENTER);

        JButton addLineButton = UiUtils.toolbarButton("+ Add Line");
        addLineButton.addActionListener(action -> addLine());
        JPanel addRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        addRow.setOpaque(false);
        addRow.add(addLineButton);
        addRow.add(javax.swing.Box.createHorizontalStrut(10));
        addRow.add(UiUtils.subtitleLabel(
                "At least one line is required. Use the Remove button to delete a line."));

        JPanel lineSection = new JPanel(new BorderLayout(0, 8));
        lineSection.setOpaque(false);
        lineSection.add(linesCard, BorderLayout.CENTER);
        lineSection.add(addRow, BorderLayout.SOUTH);

        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setOpaque(false);
        content.add(form, BorderLayout.NORTH);
        content.add(lineSection, BorderLayout.CENTER);

        addLine();

        initialize(content, "Create Draft", () -> execute("create the purchase order draft",
                () -> handler.save(readForm())));
        centerWithSize(820, 600);
        setResizable(true);
        setMinimumSize(new Dimension(760, 520));
    }

    private JPanel buildLineHeader() {
        JPanel header = new JPanel(new GridBagLayout());
        header.setOpaque(false);
        addCell(header, "Product", 0, 1.0, GridBagConstraints.HORIZONTAL);
        addCell(header, "Quantity", 1, 0.0, GridBagConstraints.NONE);
        addCell(header, "Unit Cost (¥)", 2, 0.0, GridBagConstraints.NONE);
        addCell(header, "", 3, 0.0, GridBagConstraints.NONE);
        return header;
    }

    private void addCell(JPanel panel, String text, int gridx, double weightx, int fill) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = gridx;
        constraints.gridy = 0;
        constraints.weightx = weightx;
        constraints.fill = fill;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, gridx == 0 ? 4 : 8, 4, 0);
        JLabel label = new JLabel(text);
        label.setFont(Theme.FONT_SMALL_BOLD);
        label.setForeground(Theme.MUTED);
        panel.add(label, constraints);
    }

    private void addLine() {
        LineRow row = new LineRow();
        lineRows.add(row);
        linesPanel.add(row.panel);
        if (lineRows.size() > 1) {
            linesPanel.add(javax.swing.Box.createVerticalStrut(4));
        }
        refreshLineButtons();
        linesPanel.revalidate();
        linesPanel.repaint();
        row.panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
    }

    private void removeLine(LineRow row) {
        if (lineRows.size() <= 1) {
            UiUtils.warn(this, "A purchase order must contain at least one product line.");
            return;
        }
        lineRows.remove(row);
        linesPanel.removeAll();
        for (LineRow remaining : lineRows) {
            linesPanel.add(remaining.panel);
            linesPanel.add(javax.swing.Box.createVerticalStrut(4));
        }
        refreshLineButtons();
        linesPanel.revalidate();
        linesPanel.repaint();
    }

    private void refreshLineButtons() {
        for (LineRow row : lineRows) {
            row.removeButton.setEnabled(lineRows.size() > 1);
        }
    }

    private DraftData readForm() throws ValidationException {
        Supplier supplier = (Supplier) supplierCombo.getSelectedItem();
        if (supplier == null) {
            throw new ValidationException("Please select a supplier for the purchase order.");
        }
        LocalDate expectedDate = UiUtils.parseOptionalDate(expectedDateField.getText(),
                "Expected delivery date");
        if (lineRows.isEmpty()) {
            throw new ValidationException("Add at least one product line.");
        }
        List<OrderLineDraft> drafts = new ArrayList<>();
        for (LineRow row : lineRows) {
            Product product = (Product) row.productCombo.getSelectedItem();
            if (product == null) {
                throw new ValidationException("Please select a product for every order line.");
            }
            int quantity = UiUtils.parseInt(row.quantityField.getText(), "Order quantity");
            if (quantity <= 0) {
                throw new ValidationException("Order quantity for " + product.getSku()
                        + " must be a positive whole number.");
            }
            BigDecimal unitCost = UiUtils.parseDecimal(row.costField.getText(), "Unit cost");
            if (unitCost.signum() < 0) {
                throw new ValidationException("Unit cost for " + product.getSku()
                        + " must be zero or positive.");
            }
            drafts.add(new OrderLineDraft(product.getId(), quantity, unitCost));
        }
        return new DraftData(supplier.getId(), expectedDate, noteField.getText(), drafts);
    }

    /** One editable product line of the draft. */
    private final class LineRow {
        private final JPanel panel = new JPanel(new GridBagLayout());
        private final JComboBox<Product> productCombo;
        private final JTextField quantityField = new JTextField("1", 5);
        private final JTextField costField = new JTextField(8);
        private final JButton removeButton = UiUtils.toolbarButton("Remove");

        private LineRow() {
            panel.setOpaque(false);
            productCombo = new JComboBox<>(products.toArray(new Product[0]));
            productCombo.setRenderer(new DisplayRenderer<Product>(
                    product -> product == null ? "" : product.getSku() + " - " + product.getName()));
            productCombo.addActionListener(action -> fillUnitCost());
            if (productCombo.getItemCount() > 0) {
                productCombo.setSelectedIndex(0);
                fillUnitCost();
            }
            removeButton.addActionListener(action -> removeLine(this));

            GridBagConstraints productConstraints = new GridBagConstraints();
            productConstraints.gridx = 0;
            productConstraints.gridy = 0;
            productConstraints.weightx = 1.0;
            productConstraints.fill = GridBagConstraints.HORIZONTAL;
            productConstraints.insets = new Insets(2, 4, 2, 8);
            panel.add(productCombo, productConstraints);

            GridBagConstraints quantityConstraints = new GridBagConstraints();
            quantityConstraints.gridx = 1;
            quantityConstraints.gridy = 0;
            quantityConstraints.insets = new Insets(2, 0, 2, 8);
            panel.add(quantityField, quantityConstraints);

            GridBagConstraints costConstraints = new GridBagConstraints();
            costConstraints.gridx = 2;
            costConstraints.gridy = 0;
            costConstraints.insets = new Insets(2, 0, 2, 8);
            panel.add(costField, costConstraints);

            GridBagConstraints removeConstraints = new GridBagConstraints();
            removeConstraints.gridx = 3;
            removeConstraints.gridy = 0;
            removeConstraints.insets = new Insets(2, 0, 2, 4);
            panel.add(removeButton, removeConstraints);
        }

        private void fillUnitCost() {
            Product product = (Product) productCombo.getSelectedItem();
            if (product != null) {
                costField.setText(product.getUnitPrice().toPlainString());
            }
        }
    }
}
