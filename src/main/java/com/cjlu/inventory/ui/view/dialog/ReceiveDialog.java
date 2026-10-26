package com.cjlu.inventory.ui.view.dialog;

import com.cjlu.inventory.exception.ValidationException;
import com.cjlu.inventory.model.PurchaseOrder;
import com.cjlu.inventory.model.PurchaseOrderLine;
import com.cjlu.inventory.ui.util.Theme;
import com.cjlu.inventory.ui.util.UiUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Receives a delivery against an ordered (or partially received) purchase
 * order. Each line accepts a quantity between zero and the outstanding
 * quantity, so partial deliveries are recorded naturally.
 */
public class ReceiveDialog extends BaseDialog {

    private static final long serialVersionUID = 1L;

    private final PurchaseOrder order;
    private final Map<String, JTextField> quantityFields = new LinkedHashMap<>();
    private final Map<String, PurchaseOrderLine> linesById = new LinkedHashMap<>();

    public ReceiveDialog(Window owner, PurchaseOrder order, String supplierName,
                         Map<String, String> productSkus, Map<String, String> productNames,
                         DialogHandler<Map<String, Integer>> handler) {
        super(owner, "Receive Purchase Order");
        this.order = order;

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setOpaque(false);

        JPanel summary = new JPanel(new BorderLayout(0, 4));
        summary.setOpaque(false);
        JLabel header = new JLabel(order.getOrderNumber() + "  -  " + UiUtils.orDash(supplierName));
        header.setFont(Theme.FONT_SECTION);
        header.setForeground(Theme.PRIMARY);
        summary.add(header, BorderLayout.NORTH);
        summary.add(UiUtils.subtitleLabel("Ordered " + order.getTotalOrderedQuantity()
                + " unit(s), already received " + order.getTotalReceivedQuantity()
                + ". Enter the quantity delivered in this delivery."), BorderLayout.CENTER);

        JPanel rows = new JPanel();
        rows.setLayout(new javax.swing.BoxLayout(rows, javax.swing.BoxLayout.Y_AXIS));
        rows.setBackground(Theme.CARD);
        List<PurchaseOrderLine> lines = order.getLines();
        for (PurchaseOrderLine line : lines) {
            linesById.put(line.getId(), line);
            JPanel row = buildLineRow(line, productSkus, productNames);
            rows.add(row);
            rows.add(javax.swing.Box.createVerticalStrut(4));
        }
        JScrollPane scroll = UiUtils.scroll(rows);
        scroll.setPreferredSize(new Dimension(700, 230));

        JPanel linesCard = new JPanel(new BorderLayout());
        linesCard.setOpaque(false);
        linesCard.add(scroll, BorderLayout.CENTER);

        JButton fillAllButton = UiUtils.toolbarButton("Fill All Remaining");
        fillAllButton.addActionListener(action -> fillAllRemaining());
        JPanel hintRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        hintRow.setOpaque(false);
        hintRow.add(fillAllButton);
        hintRow.add(javax.swing.Box.createHorizontalStrut(10));
        hintRow.add(UiUtils.subtitleLabel("Leave a line at 0 when it is not part of this delivery."));

        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setOpaque(false);
        center.add(linesCard, BorderLayout.CENTER);
        center.add(hintRow, BorderLayout.SOUTH);

        content.add(summary, BorderLayout.NORTH);
        content.add(center, BorderLayout.CENTER);

        initialize(content, "Receive", () -> execute("receive the purchase order",
                () -> handler.save(readForm())));
        centerWithSize(800, 520);
        setResizable(true);
        setMinimumSize(new Dimension(720, 440));
    }

    private JPanel buildLineRow(PurchaseOrderLine line, Map<String, String> productSkus,
                                Map<String, String> productNames) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)));

        String sku = productSkus.getOrDefault(line.getProductId(), "-");
        String name = productNames.getOrDefault(line.getProductId(), "Unknown product");
        JLabel label = new JLabel(sku + " - " + name + "   (ordered " + line.getQuantity()
                + ", received " + line.getReceivedQuantity()
                + ", remaining " + line.getRemainingQuantity() + ")");
        label.setFont(Theme.FONT_BASE);
        row.add(label, BorderLayout.CENTER);

        JTextField field = new JTextField("0", 6);
        field.setHorizontalAlignment(JTextField.RIGHT);
        field.setFont(Theme.FONT_BASE);
        quantityFields.put(line.getId(), field);
        row.add(field, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        return row;
    }

    private void fillAllRemaining() {
        for (PurchaseOrderLine line : order.getLines()) {
            JTextField field = quantityFields.get(line.getId());
            if (field != null) {
                field.setText(Integer.toString(line.getRemainingQuantity()));
            }
        }
    }

    private Map<String, Integer> readForm() throws ValidationException {
        Map<String, Integer> quantities = new LinkedHashMap<>();
        boolean anyReceived = false;
        for (Map.Entry<String, JTextField> entry : quantityFields.entrySet()) {
            PurchaseOrderLine line = linesById.get(entry.getKey());
            if (line == null) {
                continue;
            }
            int quantity = UiUtils.parseInt(entry.getValue().getText(),
                    "Received quantity for " + line.getProductId());
            if (quantity < 0) {
                throw new ValidationException("Received quantities must be zero or positive.");
            }
            if (quantity > line.getRemainingQuantity()) {
                throw new ValidationException("Cannot receive " + quantity + " unit(s): only "
                        + line.getRemainingQuantity() + " remain on this line.");
            }
            if (quantity > 0) {
                anyReceived = true;
            }
            quantities.put(line.getId(), quantity);
        }
        if (!anyReceived) {
            throw new ValidationException("Enter a received quantity for at least one line.");
        }
        return quantities;
    }
}
