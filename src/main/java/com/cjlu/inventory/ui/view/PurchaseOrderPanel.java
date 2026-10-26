package com.cjlu.inventory.ui.view;

import com.cjlu.inventory.app.ApplicationContext;
import com.cjlu.inventory.model.PurchaseOrder;
import com.cjlu.inventory.model.PurchaseOrderStatus;
import com.cjlu.inventory.ui.component.DisplayRenderer;
import com.cjlu.inventory.ui.controller.PurchaseOrderController;
import com.cjlu.inventory.ui.mediator.ViewMediator;
import com.cjlu.inventory.ui.table.PurchaseOrderLineTableModel;
import com.cjlu.inventory.ui.table.PurchaseOrderTableModel;
import com.cjlu.inventory.ui.util.Theme;
import com.cjlu.inventory.ui.util.UiUtils;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Purchase Orders tab (the core new Project 2 feature): order list with status
 * filter, draft creation (manual or generated from low stock), submit / cancel
 * (administrator), receiving (including partial deliveries) and an order
 * detail panel with its product lines.
 */
public class PurchaseOrderPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final String[] DETAIL_FIELDS = {
            "Order No.", "Supplier", "Status", "Created By", "Created On", "Expected",
            "Total", "Received At", "Note"
    };

    private final ApplicationContext context;
    private final PurchaseOrderController controller;
    private final JComboBox<PurchaseOrderStatus> statusCombo = new JComboBox<>();
    private final PurchaseOrderTableModel orderModel = new PurchaseOrderTableModel();
    private final JTable orderTable = new JTable(orderModel);
    private final PurchaseOrderLineTableModel lineModel = new PurchaseOrderLineTableModel();
    private final JTable lineTable = new JTable(lineModel);
    private final Map<String, JLabel> detailValues = new LinkedHashMap<>();
    private final JLabel countLabel = UiUtils.subtitleLabel("0 purchase orders");
    private JSplitPane splitPane;

    public PurchaseOrderPanel(ApplicationContext context, ViewMediator mediator) {
        this.context = context;
        setLayout(new BorderLayout(0, 12));
        setOpaque(true);
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        statusCombo.setRenderer(new DisplayRenderer<PurchaseOrderStatus>(
                status -> status == null ? "All statuses" : status.getLabel()));
        statusCombo.addItem(null);
        for (PurchaseOrderStatus status : PurchaseOrderStatus.values()) {
            statusCombo.addItem(status);
        }
        statusCombo.setPreferredSize(new Dimension(170, 28));

        UiUtils.styleTable(orderTable);
        orderTable.setAutoCreateRowSorter(true);
        orderTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        int[] orderWidths = {160, 200, 140, 105, 140, 100, 110, 90};
        for (int index = 0; index < orderWidths.length; index++) {
            orderTable.getColumnModel().getColumn(index).setPreferredWidth(orderWidths[index]);
        }
        orderTable.getColumnModel().getColumn(2)
                .setCellRenderer(new com.cjlu.inventory.ui.component.StatusTableCellRenderer());

        UiUtils.styleTable(lineTable);
        lineTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        int[] lineWidths = {90, 230, 80, 80, 90, 100, 110};
        for (int index = 0; index < lineWidths.length; index++) {
            lineTable.getColumnModel().getColumn(index).setPreferredWidth(lineWidths[index]);
        }

        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);

        controller = new PurchaseOrderController(context, this, mediator);
        wireEvents();
        controller.initialize();
    }

    private JPanel buildHeader() {
        JPanel headerCard = UiUtils.card();
        headerCard.setLayout(new BorderLayout(0, 8));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(UiUtils.titleLabel("Purchase Orders"));
        titles.add(Box.createVerticalStrut(2));
        titles.add(UiUtils.subtitleLabel(
                "Prepare, submit and receive replenishment orders for low-stock products."));
        titleRow.add(titles, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton newDraftButton = UiUtils.primaryButton("+ New Draft");
        JButton lowStockButton = UiUtils.button("From Low Stock");
        JButton submitButton = UiUtils.button("Submit");
        JButton cancelButton = UiUtils.button("Cancel Order");
        JButton receiveButton = UiUtils.button("Receive");
        JButton refreshButton = UiUtils.button("Refresh");
        newDraftButton.addActionListener(action -> controller.createDraft());
        lowStockButton.addActionListener(action -> controller.generateFromLowStock());
        submitButton.addActionListener(action -> controller.submitSelected());
        cancelButton.addActionListener(action -> controller.cancelSelected());
        receiveButton.addActionListener(action -> controller.receiveSelected());
        refreshButton.addActionListener(action -> controller.refresh());
        boolean admin = context.getAuthService().isAdmin();
        submitButton.setVisible(admin);
        cancelButton.setVisible(admin);
        actions.add(newDraftButton);
        actions.add(lowStockButton);
        actions.add(submitButton);
        actions.add(cancelButton);
        actions.add(receiveButton);
        actions.add(refreshButton);
        titleRow.add(actions, BorderLayout.EAST);
        headerCard.add(titleRow, BorderLayout.NORTH);

        JPanel filterRow = new JPanel(new BorderLayout());
        filterRow.setOpaque(false);
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);
        JButton applyButton = UiUtils.toolbarButton("Apply Filter");
        applyButton.addActionListener(action -> controller.refresh());
        filters.add(UiUtils.mutedLabel("Status:"));
        filters.add(statusCombo);
        filters.add(applyButton);
        filterRow.add(filters, BorderLayout.WEST);
        filterRow.add(countLabel, BorderLayout.EAST);
        headerCard.add(filterRow, BorderLayout.SOUTH);
        return headerCard;
    }

    private JComponent buildCenter() {
        JPanel orderCard = UiUtils.titledCard("Purchase Orders", UiUtils.scroll(orderTable));
        JPanel detailCard = buildDetailCard();
        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, orderCard, detailCard);
        splitPane.setResizeWeight(0.45);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        splitPane.setDividerSize(8);
        return splitPane;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        // The divider can only be positioned proportionally once the panel has a size.
        SwingUtilities.invokeLater(() -> {
            if (splitPane != null && splitPane.getHeight() > 0) {
                splitPane.setDividerLocation(0.40);
            }
        });
    }

    private JPanel buildDetailCard() {
        JPanel card = UiUtils.card();
        card.setLayout(new BorderLayout(0, 8));
        card.add(UiUtils.sectionTitle("Order Details"), BorderLayout.NORTH);

        JPanel summary = new JPanel(new GridBagLayout());
        summary.setOpaque(false);
        int fieldIndex = 0;
        for (String field : DETAIL_FIELDS) {
            JLabel name = new JLabel(field + ":");
            name.setFont(Theme.FONT_SMALL_BOLD);
            name.setForeground(Theme.MUTED);
            JLabel value = new JLabel("-");
            value.setFont(Theme.FONT_BASE);
            value.setForeground(Theme.TEXT);
            detailValues.put(field, value);

            int column = (fieldIndex % 3) * 2;
            int row = fieldIndex / 3;
            GridBagConstraints nameConstraints = new GridBagConstraints();
            nameConstraints.gridx = column;
            nameConstraints.gridy = row;
            nameConstraints.anchor = GridBagConstraints.WEST;
            nameConstraints.insets = new Insets(3, 0, 3, 8);
            summary.add(name, nameConstraints);

            GridBagConstraints valueConstraints = new GridBagConstraints();
            valueConstraints.gridx = column + 1;
            valueConstraints.gridy = row;
            valueConstraints.weightx = 0.34;
            valueConstraints.fill = GridBagConstraints.HORIZONTAL;
            valueConstraints.anchor = GridBagConstraints.WEST;
            valueConstraints.insets = new Insets(3, 0, 3, 20);
            summary.add(value, valueConstraints);
            fieldIndex++;
        }

        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setOpaque(false);
        body.add(summary, BorderLayout.NORTH);

        JPanel linesSection = new JPanel(new BorderLayout(0, 6));
        linesSection.setOpaque(false);
        linesSection.add(UiUtils.sectionTitle("Order Lines"), BorderLayout.NORTH);
        linesSection.add(UiUtils.scroll(lineTable), BorderLayout.CENTER);
        body.add(linesSection, BorderLayout.CENTER);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private void wireEvents() {
        orderTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                controller.onOrderSelected(getSelectedOrder());
            }
        });
    }

    /** Reloads the order list with the current status filter. */
    public void refresh() {
        controller.refresh();
    }

    // ------------------------------------------------- view API for controller

    public PurchaseOrder getSelectedOrder() {
        int viewRow = orderTable.getSelectedRow();
        if (viewRow < 0) {
            return null;
        }
        int modelRow = orderTable.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= orderModel.getRowCount()) {
            return null;
        }
        return orderModel.getOrderAt(modelRow);
    }

    /** The status currently chosen in the filter combo; {@code null} means all. */
    public PurchaseOrderStatus getStatusFilter() {
        Object selected = statusCombo.getSelectedItem();
        return selected instanceof PurchaseOrderStatus status ? status : null;
    }

    public void setOrders(List<PurchaseOrder> orders, Map<String, String> supplierNames) {
        orderModel.setSupplierNames(supplierNames);
        orderModel.setOrders(orders);
        orderTable.clearSelection();
        countLabel.setText(orders.size() + " purchase order(s)");
    }

    /** Fills the detail panel with the header values and lines of one order. */
    public void setOrderDetails(PurchaseOrder order, Map<String, String> productSkus,
                                Map<String, String> productNames) {
        if (order == null) {
            clearOrderDetails();
            return;
        }
        setDetailValue("Order No.", order.getOrderNumber());
        setDetailValue("Supplier", supplierNameOf(order));
        setDetailValue("Status", order.getStatus().getLabel());
        setDetailValue("Created By", order.getCreatedBy());
        setDetailValue("Created On", UiUtils.formatDateTime(order.getCreatedOn()));
        setDetailValue("Expected", UiUtils.formatDate(order.getExpectedDate()));
        setDetailValue("Total", UiUtils.formatMoney(order.getTotalValue()));
        setDetailValue("Received At", UiUtils.formatDateTime(order.getReceivedAt()));
        setDetailValue("Note", UiUtils.orDash(order.getNote()));
        lineModel.setProductLookups(productSkus, productNames);
        lineModel.setLines(order.getLines());
    }

    /** Clears the order detail panel. */
    public void clearOrderDetails() {
        for (JLabel value : detailValues.values()) {
            value.setText("-");
        }
        lineModel.setLines(List.of());
    }

    /** Selects the order with the given id after a refresh, if it is still visible. */
    public void selectOrderById(String orderId) {
        if (orderId == null) {
            return;
        }
        for (int modelRow = 0; modelRow < orderModel.getRowCount(); modelRow++) {
            PurchaseOrder order = orderModel.getOrderAt(modelRow);
            if (orderId.equals(order.getId())) {
                int viewRow = orderTable.convertRowIndexToView(modelRow);
                if (viewRow >= 0) {
                    orderTable.setRowSelectionInterval(viewRow, viewRow);
                    orderTable.scrollRectToVisible(orderTable.getCellRect(viewRow, 0, true));
                }
                return;
            }
        }
    }

    private void setDetailValue(String field, String value) {
        JLabel label = detailValues.get(field);
        if (label != null) {
            String text = UiUtils.orDash(value);
            label.setText(text);
            label.setToolTipText(text);
        }
    }

    private String supplierNameOf(PurchaseOrder order) {
        String name = orderModel.getSupplierName(order.getSupplierId());
        return name == null ? "Unknown supplier" : name;
    }
}
