package com.cjlu.inventory.ui.view;

import com.cjlu.inventory.app.ApplicationContext;
import com.cjlu.inventory.model.Category;
import com.cjlu.inventory.model.Product;
import com.cjlu.inventory.model.Supplier;
import com.cjlu.inventory.service.SortField;
import com.cjlu.inventory.ui.component.DisplayRenderer;
import com.cjlu.inventory.ui.controller.ProductController;
import com.cjlu.inventory.ui.table.ProductTableModel;
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
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;

/**
 * Products tab: searchable, filterable and sortable product catalogue with
 * add / edit / delete / details actions. The mutating actions are only shown
 * to administrators; the service re-checks the role as defence in depth.
 */
public class ProductPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final String[] SORT_OPTIONS = {
            "Default order", "Name (A-Z)", "Quantity (low to high)", "Inventory value (low to high)"
    };

    private final ApplicationContext context;
    private final ProductController controller;
    private final JTextField searchField = new JTextField(16);
    private final JComboBox<Category> categoryCombo = new JComboBox<>();
    private final JComboBox<String> sortCombo = new JComboBox<>(SORT_OPTIONS);
    private final ProductTableModel tableModel = new ProductTableModel();
    private final JTable table = new JTable(tableModel);
    private final JLabel countLabel = UiUtils.subtitleLabel("0 products");
    private boolean updatingFilters;

    public ProductPanel(ApplicationContext context) {
        this.context = context;
        setLayout(new BorderLayout(0, 12));
        setOpaque(true);
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        categoryCombo.setRenderer(new DisplayRenderer<Category>(
                category -> category == null ? "All categories" : category.getName()));
        categoryCombo.setPreferredSize(new Dimension(170, 28));
        sortCombo.setPreferredSize(new Dimension(190, 28));

        table.setAutoCreateRowSorter(true);
        ((TableRowSorter<?>) table.getRowSorter()).setSortsOnUpdates(true);
        UiUtils.styleTable(table);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        int[] widths = {90, 190, 130, 160, 55, 100, 90, 110, 110};
        for (int index = 0; index < widths.length; index++) {
            table.getColumnModel().getColumn(index).setPreferredWidth(widths[index]);
        }
        table.getColumnModel().getColumn(8)
                .setCellRenderer(new com.cjlu.inventory.ui.component.StatusTableCellRenderer());

        add(buildHeader(), BorderLayout.NORTH);
        add(buildTableCard(), BorderLayout.CENTER);

        controller = new ProductController(context, this);
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
        titles.add(UiUtils.titleLabel("Products"));
        titles.add(Box.createVerticalStrut(2));
        titles.add(UiUtils.subtitleLabel(context.getAuthService().isAdmin()
                ? "Create, edit and delete catalogue products, or inspect their details."
                : "Read-only catalogue view - changes require an administrator account."));
        titleRow.add(titles, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton detailsButton = UiUtils.button("Details");
        JButton editButton = UiUtils.button("Edit");
        JButton deleteButton = UiUtils.button("Delete");
        JButton addButton = UiUtils.primaryButton("+ New Product");
        detailsButton.addActionListener(action -> controller.showDetails());
        editButton.addActionListener(action -> controller.showEditDialog());
        deleteButton.addActionListener(action -> controller.deleteSelected());
        addButton.addActionListener(action -> controller.showAddDialog());
        boolean admin = context.getAuthService().isAdmin();
        editButton.setVisible(admin);
        deleteButton.setVisible(admin);
        addButton.setVisible(admin);
        actions.add(addButton);
        actions.add(editButton);
        actions.add(deleteButton);
        actions.add(detailsButton);
        titleRow.add(actions, BorderLayout.EAST);
        header.add(titleRow, BorderLayout.NORTH);

        JPanel filterRow = new JPanel(new BorderLayout());
        filterRow.setOpaque(false);
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);
        JLabel searchLabel = UiUtils.mutedLabel("Search:");
        JButton searchButton = UiUtils.toolbarButton("Search");
        JButton resetButton = UiUtils.toolbarButton("Reset");
        JLabel categoryLabel = UiUtils.mutedLabel("Category:");
        JLabel sortLabel = UiUtils.mutedLabel("Sort:");
        JButton refreshButton = UiUtils.toolbarButton("Refresh");
        filters.add(searchLabel);
        filters.add(searchField);
        filters.add(searchButton);
        filters.add(categoryLabel);
        filters.add(categoryCombo);
        filters.add(sortLabel);
        filters.add(sortCombo);
        filters.add(resetButton);
        filters.add(refreshButton);
        filterRow.add(filters, BorderLayout.WEST);
        filterRow.add(countLabel, BorderLayout.EAST);
        header.add(filterRow, BorderLayout.SOUTH);

        searchButton.addActionListener(action -> controller.applyFilters());
        searchField.addActionListener(action -> controller.applyFilters());
        resetButton.addActionListener(action -> resetFilters());
        refreshButton.addActionListener(action -> controller.refresh());
        return header;
    }

    private JPanel buildTableCard() {
        JPanel card = UiUtils.card();
        card.setLayout(new BorderLayout());
        card.add(UiUtils.scroll(table), BorderLayout.CENTER);
        return card;
    }

    private void wireEvents() {
        categoryCombo.addActionListener(action -> {
            if (!updatingFilters) {
                controller.applyFilters();
            }
        });
        sortCombo.addActionListener(action -> {
            if (!updatingFilters) {
                controller.applyFilters();
            }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    if (context.getAuthService().isAdmin()) {
                        controller.showEditDialog();
                    } else {
                        controller.showDetails();
                    }
                }
            }
        });
    }

    private void resetFilters() {
        updatingFilters = true;
        try {
            searchField.setText("");
            if (categoryCombo.getItemCount() > 0) {
                categoryCombo.setSelectedIndex(0);
            }
            sortCombo.setSelectedIndex(0);
        } finally {
            updatingFilters = false;
        }
        controller.applyFilters();
    }

    /** Reloads the table (called when the tab becomes visible / after stock changes). */
    public void refresh() {
        controller.refresh();
    }

    // ------------------------------------------------- view API for controller

    public String getSearchText() {
        return searchField.getText();
    }

    public String getSelectedCategoryId() {
        Object selected = categoryCombo.getSelectedItem();
        return selected instanceof Category category ? category.getId() : null;
    }

    public SortField getSelectedSortField() {
        return switch (sortCombo.getSelectedIndex()) {
            case 1 -> SortField.NAME;
            case 2 -> SortField.QUANTITY;
            case 3 -> SortField.VALUE;
            default -> null;
        };
    }

    /** The product of the currently selected row, or {@code null}. */
    public Product getSelectedProduct() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return null;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= tableModel.getRowCount()) {
            return null;
        }
        return tableModel.getProductAt(modelRow);
    }

    public void setProducts(List<Product> products) {
        tableModel.setProducts(products);
    }

    public void setLookups(Map<String, String> categoryNames, Map<String, String> supplierNames) {
        tableModel.setLookups(categoryNames, supplierNames);
    }

    /** Replaces the category filter choices while keeping the current selection. */
    public void setCategories(List<Category> categories) {
        String selectedId = getSelectedCategoryId();
        updatingFilters = true;
        try {
            DefaultComboBoxModel<Category> model = new DefaultComboBoxModel<>();
            model.addElement(null);
            for (Category category : categories) {
                model.addElement(category);
            }
            categoryCombo.setModel(model);
            if (selectedId != null) {
                for (int index = 1; index < model.getSize(); index++) {
                    Category candidate = model.getElementAt(index);
                    if (candidate != null && selectedId.equals(candidate.getId())) {
                        categoryCombo.setSelectedIndex(index);
                        return;
                    }
                }
            }
            categoryCombo.setSelectedIndex(0);
        } finally {
            updatingFilters = false;
        }
    }

    public void setCountLabel(int count) {
        countLabel.setText(count + " product(s)");
    }
}
