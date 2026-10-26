package com.cjlu.inventory.ui.view;

import com.cjlu.inventory.app.ApplicationContext;
import com.cjlu.inventory.model.Product;
import com.cjlu.inventory.model.StockTransaction;
import com.cjlu.inventory.ui.component.DisplayRenderer;
import com.cjlu.inventory.ui.controller.StockController;
import com.cjlu.inventory.ui.table.TransactionTableModel;
import com.cjlu.inventory.ui.util.Theme;
import com.cjlu.inventory.ui.util.UiUtils;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;

/**
 * Stock tab: selects a product (with keyword search), shows its audit history
 * and offers stock-in, stock-out and (administrator only) counted-quantity
 * adjustments. The history can be narrowed with a date range.
 */
public class StockPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final ApplicationContext context;
    private final StockController controller;
    private final JComboBox<Product> productCombo = new JComboBox<>();
    private final JTextField searchField = new JTextField(12);
    private final JLabel productInfoLabel = UiUtils.mutedLabel("No product selected.");
    private final JTextField fromDateField = new JTextField(10);
    private final JTextField toDateField = new JTextField(10);
    private final JLabel historyCountLabel = UiUtils.subtitleLabel("0 transactions");
    private final TransactionTableModel tableModel = new TransactionTableModel();
    private final JTable table = new JTable(tableModel);
    private boolean updatingChoices;

    public StockPanel(ApplicationContext context) {
        this.context = context;
        setLayout(new BorderLayout(0, 12));
        setOpaque(true);
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        productCombo.setRenderer(new DisplayRenderer<Product>(
                product -> product == null ? ""
                        : product.getSku() + " - " + product.getName()));
        productCombo.setPreferredSize(new Dimension(330, 28));
        UiUtils.styleTable(table);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.getColumnModel().getColumn(0).setPreferredWidth(110);
        table.getColumnModel().getColumn(1).setPreferredWidth(90);
        table.getColumnModel().getColumn(2).setPreferredWidth(120);
        table.getColumnModel().getColumn(3).setPreferredWidth(160);
        table.getColumnModel().getColumn(4).setPreferredWidth(360);

        add(buildHeader(), BorderLayout.NORTH);
        add(UiUtils.titledCard("Transaction History", UiUtils.scroll(table)), BorderLayout.CENTER);

        controller = new StockController(context, this);
        wireEvents();
        controller.initialize();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 10));
        header.setOpaque(false);

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(UiUtils.titleLabel("Stock Movements"));
        titles.add(Box.createVerticalStrut(2));
        titles.add(UiUtils.subtitleLabel(
                "Record stock-in and stock-out movements and review the audit history."));
        titleRow.add(titles, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton refreshButton = UiUtils.button("Refresh");
        JButton stockInButton = UiUtils.primaryButton("Stock In");
        JButton stockOutButton = UiUtils.button("Stock Out");
        JButton adjustButton = UiUtils.button("Adjust Stock");
        refreshButton.addActionListener(action -> controller.refresh());
        stockInButton.addActionListener(action -> controller.stockIn());
        stockOutButton.addActionListener(action -> controller.stockOut());
        adjustButton.addActionListener(action -> controller.adjustStock());
        adjustButton.setVisible(context.getAuthService().isAdmin());
        actions.add(stockInButton);
        actions.add(stockOutButton);
        actions.add(adjustButton);
        actions.add(refreshButton);
        titleRow.add(actions, BorderLayout.EAST);
        header.add(titleRow, BorderLayout.NORTH);

        JPanel selectorRow = new JPanel(new BorderLayout());
        selectorRow.setOpaque(false);
        JPanel selector = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        selector.setOpaque(false);
        JButton findButton = UiUtils.toolbarButton("Find");
        JButton clearSearchButton = UiUtils.toolbarButton("Show All");
        findButton.addActionListener(action -> controller.refreshProductChoices());
        searchField.addActionListener(action -> controller.refreshProductChoices());
        clearSearchButton.addActionListener(action -> {
            searchField.setText("");
            controller.refreshProductChoices();
        });
        selector.add(UiUtils.mutedLabel("Product:"));
        selector.add(productCombo);
        selector.add(UiUtils.mutedLabel("Search:"));
        selector.add(searchField);
        selector.add(findButton);
        selector.add(clearSearchButton);
        selectorRow.add(selector, BorderLayout.WEST);
        header.add(selectorRow, BorderLayout.SOUTH);

        JPanel infoRow = new JPanel(new BorderLayout());
        infoRow.setOpaque(false);
        infoRow.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        infoRow.add(productInfoLabel, BorderLayout.WEST);
        header.add(infoRow, BorderLayout.CENTER);

        JPanel filterRow = new JPanel(new BorderLayout());
        filterRow.setOpaque(false);
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);
        JButton applyButton = UiUtils.toolbarButton("Apply Filter");
        JButton clearButton = UiUtils.toolbarButton("Clear Filter");
        applyButton.addActionListener(action -> controller.applyDateFilter());
        clearButton.addActionListener(action -> controller.clearDateFilter());
        filters.add(UiUtils.mutedLabel("From (yyyy-MM-dd):"));
        filters.add(fromDateField);
        filters.add(UiUtils.mutedLabel("To (yyyy-MM-dd):"));
        filters.add(toDateField);
        filters.add(applyButton);
        filters.add(clearButton);
        filterRow.add(filters, BorderLayout.WEST);
        filterRow.add(historyCountLabel, BorderLayout.EAST);

        JPanel headerCard = UiUtils.card();
        headerCard.setLayout(new BorderLayout());
        headerCard.add(header, BorderLayout.CENTER);
        headerCard.add(filterRow, BorderLayout.SOUTH);
        return headerCard;
    }

    private void wireEvents() {
        productCombo.addActionListener(action -> {
            if (!updatingChoices) {
                controller.onProductSelected(getSelectedProduct());
            }
        });
    }

    /** Reloads the product choices and the selected product's history. */
    public void refresh() {
        controller.refresh();
    }

    // ------------------------------------------------- view API for controller

    public String getProductSearchText() {
        return searchField.getText();
    }

    public Product getSelectedProduct() {
        Object selected = productCombo.getSelectedItem();
        return selected instanceof Product product ? product : null;
    }

    /** Replaces the product choices without firing selection callbacks. */
    public void setProductChoices(List<Product> products, String preferredProductId) {
        updatingChoices = true;
        try {
            DefaultComboBoxModel<Product> model = new DefaultComboBoxModel<>();
            for (Product product : products) {
                model.addElement(product);
            }
            productCombo.setModel(model);
            if (preferredProductId != null) {
                for (int index = 0; index < model.getSize(); index++) {
                    Product candidate = model.getElementAt(index);
                    if (candidate != null && preferredProductId.equals(candidate.getId())) {
                        productCombo.setSelectedIndex(index);
                        return;
                    }
                }
            }
            if (model.getSize() > 0) {
                productCombo.setSelectedIndex(0);
            }
        } finally {
            updatingChoices = false;
        }
    }

    public void setProductInfo(Product product) {
        if (product == null) {
            productInfoLabel.setText("No product selected.");
            return;
        }
        productInfoLabel.setText("<html><b>" + product.getSku() + " - " + product.getName()
                + "</b>&nbsp;&nbsp;|&nbsp;&nbsp;On hand: <b>" + product.getQuantity() + " "
                + product.getUnit() + "</b>&nbsp;&nbsp;|&nbsp;&nbsp;Reorder level: "
                + product.getReorderLevel() + " " + product.getUnit()
                + "&nbsp;&nbsp;|&nbsp;&nbsp;Status: "
                + product.getStockStatus().getLabel() + "</html>");
    }

    public void setHistory(List<StockTransaction> transactions) {
        tableModel.setTransactions(transactions);
    }

    public void setHistoryCount(int count) {
        historyCountLabel.setText(count + " transaction(s)");
    }

    public String getFromDateText() {
        return fromDateField.getText();
    }

    public String getToDateText() {
        return toDateField.getText();
    }

    public void clearDateFilterFields() {
        fromDateField.setText("");
        toDateField.setText("");
    }
}
