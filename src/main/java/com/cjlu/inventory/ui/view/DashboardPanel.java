package com.cjlu.inventory.ui.view;

import com.cjlu.inventory.app.ApplicationContext;
import com.cjlu.inventory.ui.component.BarChartPanel;
import com.cjlu.inventory.ui.component.KpiCard;
import com.cjlu.inventory.ui.controller.DashboardController;
import com.cjlu.inventory.ui.controller.DashboardController.DashboardData;
import com.cjlu.inventory.ui.table.ProductTableModel;
import com.cjlu.inventory.ui.util.Theme;
import com.cjlu.inventory.ui.util.UiUtils;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Dashboard tab: KPI cards (product count, inventory value, low/out-of-stock
 * count, open purchase orders), the low-stock product table and a self-drawn
 * bar chart of the inventory value per category.
 */
public class DashboardPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final DashboardController controller;
    private final KpiCard productsCard;
    private final KpiCard valueCard;
    private final KpiCard lowStockCard;
    private final KpiCard ordersCard;
    private final JTable lowStockTable;
    private final ProductTableModel lowStockModel = new ProductTableModel();
    private final BarChartPanel chartPanel = new BarChartPanel();
    private final JLabel updatedLabel = UiUtils.subtitleLabel("Not loaded yet");

    public DashboardPanel(ApplicationContext context) {
        this.controller = new DashboardController(context);
        setLayout(new BorderLayout(0, 14));
        setOpaque(true);
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        productsCard = new KpiCard("Products", "-", "Catalogue items", Theme.PRIMARY_LIGHT);
        valueCard = new KpiCard("Inventory Value", "-", "Total stock value", Theme.SUCCESS);
        lowStockCard = new KpiCard("Low / Out of Stock", "-", "Need replenishment", Theme.WARNING);
        ordersCard = new KpiCard("Open Purchase Orders", "-", "Draft, ordered or partial",
                new Color(0x6A4C93));

        lowStockTable = new JTable(lowStockModel);
        UiUtils.styleTable(lowStockTable);
        lowStockTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        // The dashboard only shows the essential low-stock columns.
        lowStockTable.removeColumn(lowStockTable.getColumnModel().getColumn(7));
        lowStockTable.removeColumn(lowStockTable.getColumnModel().getColumn(6));
        lowStockTable.removeColumn(lowStockTable.getColumnModel().getColumn(3));
        int[] widths = {100, 210, 130, 65, 110, 115};
        for (int index = 0; index < widths.length; index++) {
            lowStockTable.getColumnModel().getColumn(index).setPreferredWidth(widths[index]);
        }
        lowStockTable.getColumnModel().getColumn(5)
                .setCellRenderer(new com.cjlu.inventory.ui.component.StatusTableCellRenderer());

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
        refresh();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(UiUtils.titleLabel("Dashboard"));
        titles.add(Box.createVerticalStrut(2));
        titles.add(UiUtils.subtitleLabel(
                "Stock health, inventory value and open purchase orders at a glance."));
        header.add(titles, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(updatedLabel);
        JButton refreshButton = UiUtils.primaryButton("Refresh");
        refreshButton.addActionListener(action -> refresh());
        actions.add(refreshButton);
        header.add(actions, BorderLayout.EAST);
        return header;
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(0, 14));
        body.setOpaque(false);

        JPanel cards = new JPanel(new GridLayout(1, 4, 14, 0));
        cards.setOpaque(false);
        cards.add(productsCard);
        cards.add(valueCard);
        cards.add(lowStockCard);
        cards.add(ordersCard);
        body.add(cards, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);

        GridBagConstraints tableConstraints = new GridBagConstraints();
        tableConstraints.gridx = 0;
        tableConstraints.gridy = 0;
        tableConstraints.weightx = 0.62;
        tableConstraints.weighty = 1.0;
        tableConstraints.fill = GridBagConstraints.BOTH;
        tableConstraints.insets = new Insets(0, 0, 0, 14);
        center.add(UiUtils.titledCard("Low Stock & Out of Stock",
                UiUtils.scroll(lowStockTable)), tableConstraints);

        GridBagConstraints chartConstraints = new GridBagConstraints();
        chartConstraints.gridx = 1;
        chartConstraints.gridy = 0;
        chartConstraints.weightx = 0.38;
        chartConstraints.weighty = 1.0;
        chartConstraints.fill = GridBagConstraints.BOTH;
        center.add(UiUtils.titledCard("Inventory Value by Category", chartPanel),
                chartConstraints);

        body.add(center, BorderLayout.CENTER);
        return body;
    }

    /** Reloads every dashboard element from the services. */
    public void refresh() {
        try {
            apply(controller.load());
        } catch (RuntimeException failure) {
            UiUtils.handle(this, "refresh the dashboard", failure);
        }
    }

    private void apply(DashboardData data) {
        productsCard.setValue(Integer.toString(data.productCount()));
        valueCard.setValue(UiUtils.formatMoney(data.totalValue()));
        lowStockCard.setValue(data.lowStockCount() + " / " + data.outOfStockCount());
        lowStockCard.setHint(data.outOfStockCount() + " currently out of stock");
        ordersCard.setValue(Integer.toString(data.openOrderCount()));
        ordersCard.setHint("Draft, ordered or partially received");

        lowStockModel.setProducts(data.lowStockProducts());
        lowStockModel.setLookups(data.categoryNames(), data.supplierNames());
        chartPanel.setData(data.valueByCategory());
        updatedLabel.setText("Last updated: " + LocalTime.now().format(TIME_FORMAT));
    }
}
