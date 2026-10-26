package com.cjlu.inventory.ui.view;

import com.cjlu.inventory.app.ApplicationContext;
import com.cjlu.inventory.model.Category;
import com.cjlu.inventory.model.Supplier;
import com.cjlu.inventory.model.User;
import com.cjlu.inventory.ui.controller.AdminController;
import com.cjlu.inventory.ui.table.CategoryTableModel;
import com.cjlu.inventory.ui.table.SupplierTableModel;
import com.cjlu.inventory.ui.table.UserTableModel;
import com.cjlu.inventory.ui.util.Theme;
import com.cjlu.inventory.ui.util.UiUtils;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;

/**
 * Administration tab (administrator only): user accounts (list, create staff,
 * reset password), category CRUD and supplier CRUD. Referential-integrity
 * failures are shown as friendly message dialogs by the controller.
 */
public class AdministrationPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final ApplicationContext context;
    private final AdminController controller;
    private final UserTableModel userModel = new UserTableModel();
    private final CategoryTableModel categoryModel = new CategoryTableModel();
    private final SupplierTableModel supplierModel = new SupplierTableModel();
    private final JTable userTable = new JTable(userModel);
    private final JTable categoryTable = new JTable(categoryModel);
    private final JTable supplierTable = new JTable(supplierModel);

    public AdministrationPanel(ApplicationContext context) {
        this.context = context;
        setLayout(new BorderLayout(0, 12));
        setOpaque(true);
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        styleTable(userTable);
        styleTable(categoryTable);
        styleTable(supplierTable);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildTabs(), BorderLayout.CENTER);

        controller = new AdminController(context, this);
        controller.initialize();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(UiUtils.titleLabel("Administration"));
        titles.add(Box.createVerticalStrut(2));
        titles.add(UiUtils.subtitleLabel(
                "User accounts, product categories and suppliers - administrator only."));
        header.add(titles, BorderLayout.WEST);
        JButton refreshButton = UiUtils.primaryButton("Refresh All");
        refreshButton.addActionListener(action -> controller.refresh());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(refreshButton);
        header.add(actions, BorderLayout.EAST);
        return header;
    }

    private JTabbedPane buildTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(Theme.FONT_BOLD);
        tabs.addTab("User Accounts", buildUsersTab());
        tabs.addTab("Categories", buildCategoriesTab());
        tabs.addTab("Suppliers", buildSuppliersTab());
        return tabs;
    }

    private JPanel buildUsersTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        JButton createButton = UiUtils.primaryButton("+ Create Staff Account");
        JButton resetButton = UiUtils.button("Reset Password");
        JButton refreshButton = UiUtils.button("Refresh");
        createButton.addActionListener(action -> controller.createStaffAccount());
        resetButton.addActionListener(action -> controller.resetPassword());
        refreshButton.addActionListener(action -> controller.refreshUsers());
        actions.add(createButton);
        actions.add(resetButton);
        actions.add(refreshButton);
        toolbar.add(actions, BorderLayout.WEST);
        toolbar.add(UiUtils.subtitleLabel("Demo accounts: admin / admin123 (Administrator), "
                + "staff / staff123 (Warehouse Staff)"), BorderLayout.EAST);
        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(UiUtils.titledCard("User Accounts", UiUtils.scroll(userTable)),
                BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildCategoriesTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        JButton addButton = UiUtils.primaryButton("+ Add Category");
        JButton editButton = UiUtils.button("Edit");
        JButton deleteButton = UiUtils.button("Delete");
        JButton refreshButton = UiUtils.button("Refresh");
        addButton.addActionListener(action -> controller.addCategory());
        editButton.addActionListener(action -> controller.editCategory());
        deleteButton.addActionListener(action -> controller.deleteCategory());
        refreshButton.addActionListener(action -> controller.refreshCategories());
        actions.add(addButton);
        actions.add(editButton);
        actions.add(deleteButton);
        actions.add(refreshButton);
        toolbar.add(actions, BorderLayout.WEST);
        toolbar.add(UiUtils.subtitleLabel(
                "Categories group the catalogue and drive the valuation chart."), BorderLayout.EAST);
        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(UiUtils.titledCard("Categories", UiUtils.scroll(categoryTable)),
                BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildSuppliersTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        JButton addButton = UiUtils.primaryButton("+ Add Supplier");
        JButton editButton = UiUtils.button("Edit");
        JButton deleteButton = UiUtils.button("Delete");
        JButton refreshButton = UiUtils.button("Refresh");
        addButton.addActionListener(action -> controller.addSupplier());
        editButton.addActionListener(action -> controller.editSupplier());
        deleteButton.addActionListener(action -> controller.deleteSupplier());
        refreshButton.addActionListener(action -> controller.refreshSuppliers());
        actions.add(addButton);
        actions.add(editButton);
        actions.add(deleteButton);
        actions.add(refreshButton);
        toolbar.add(actions, BorderLayout.WEST);
        toolbar.add(UiUtils.subtitleLabel(
                "Suppliers are referenced by products and purchase orders."), BorderLayout.EAST);
        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(UiUtils.titledCard("Suppliers", UiUtils.scroll(supplierTable)),
                BorderLayout.CENTER);
        return panel;
    }

    private void styleTable(JTable table) {
        UiUtils.styleTable(table);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
    }

    /** Reloads all administration tables. */
    public void refresh() {
        controller.refresh();
    }

    // ------------------------------------------------- view API for controller

    public User getSelectedUser() {
        int row = userTable.getSelectedRow();
        if (row < 0 || row >= userModel.getRowCount()) {
            return null;
        }
        return userModel.getUserAt(row);
    }

    public Category getSelectedCategory() {
        int row = categoryTable.getSelectedRow();
        if (row < 0 || row >= categoryModel.getRowCount()) {
            return null;
        }
        return categoryModel.getCategoryAt(row);
    }

    public Supplier getSelectedSupplier() {
        int row = supplierTable.getSelectedRow();
        if (row < 0 || row >= supplierModel.getRowCount()) {
            return null;
        }
        return supplierModel.getSupplierAt(row);
    }

    public void setUsers(List<User> users) {
        userModel.setUsers(users);
    }

    public void setCategories(List<Category> categories) {
        categoryModel.setCategories(categories);
    }

    public void setSuppliers(List<Supplier> suppliers) {
        supplierModel.setSuppliers(suppliers);
    }
}
