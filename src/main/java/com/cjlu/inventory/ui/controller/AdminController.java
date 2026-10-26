package com.cjlu.inventory.ui.controller;

import com.cjlu.inventory.app.ApplicationContext;
import com.cjlu.inventory.exception.InventoryException;
import com.cjlu.inventory.model.Category;
import com.cjlu.inventory.model.Supplier;
import com.cjlu.inventory.model.User;
import com.cjlu.inventory.ui.util.UiUtils;
import com.cjlu.inventory.ui.view.AdministrationPanel;
import com.cjlu.inventory.ui.view.dialog.AdminDialogs;

import java.util.Objects;

/**
 * Administration controller (administrator only): user accounts, categories
 * and suppliers.
 *
 * <p>Referential-integrity failures (deleting a category or supplier that is
 * still referenced by a product) are ordinary business errors and are shown as
 * friendly message dialogs.</p>
 */
public final class AdminController {

    private final ApplicationContext context;
    private final AdministrationPanel panel;

    public AdminController(ApplicationContext context, AdministrationPanel panel) {
        this.context = Objects.requireNonNull(context, "context");
        this.panel = Objects.requireNonNull(panel, "panel");
    }

    /** Loads users, categories and suppliers. */
    public void initialize() {
        refresh();
    }

    /** Reloads all three administration tables. */
    public void refresh() {
        refreshUsers();
        refreshCategories();
        refreshSuppliers();
    }

    public void refreshUsers() {
        try {
            panel.setUsers(context.getAuthService().listUsers());
        } catch (RuntimeException failure) {
            UiUtils.handle(panel, "load the user accounts", failure);
        }
    }

    public void refreshCategories() {
        try {
            panel.setCategories(context.getCatalogService().listCategories());
        } catch (RuntimeException failure) {
            UiUtils.handle(panel, "load the categories", failure);
        }
    }

    public void refreshSuppliers() {
        try {
            panel.setSuppliers(context.getCatalogService().listSuppliers());
        } catch (RuntimeException failure) {
            UiUtils.handle(panel, "load the suppliers", failure);
        }
    }

    /** Opens the create-staff-account dialog. */
    public void createStaffAccount() {
        try {
            AdminDialogs.CreateStaffDialog dialog = new AdminDialogs.CreateStaffDialog(
                    UiUtils.windowOf(panel), data -> context.getAuthService()
                            .createStaffAccount(data.username(), data.displayName(),
                                    data.password()));
            dialog.setVisible(true);
            if (dialog.isSaved()) {
                refreshUsers();
                UiUtils.info(panel, "The warehouse-staff account has been created.");
            }
        } catch (RuntimeException failure) {
            UiUtils.handle(panel, "open the create-account dialog", failure);
        }
    }

    /** Resets the password of the selected account. */
    public void resetPassword() {
        User user = panel.getSelectedUser();
        if (user == null) {
            UiUtils.info(panel, "Select a user account in the table first.");
            return;
        }
        try {
            AdminDialogs.ResetPasswordDialog dialog = new AdminDialogs.ResetPasswordDialog(
                    UiUtils.windowOf(panel), user.getUsername(), password -> context
                            .getAuthService().resetPassword(user.getUsername(), password));
            dialog.setVisible(true);
            if (dialog.isSaved()) {
                UiUtils.info(panel, "The password of '" + user.getUsername() + "' has been reset.");
            }
        } catch (RuntimeException failure) {
            UiUtils.handle(panel, "open the reset-password dialog", failure);
        }
    }

    /** Opens the category form. */
    public void addCategory() {
        try {
            AdminDialogs.CategoryDialog dialog = new AdminDialogs.CategoryDialog(
                    UiUtils.windowOf(panel), null, data -> context.getCatalogService()
                            .createCategory(data.name(), data.description()));
            dialog.setVisible(true);
            if (dialog.isSaved()) {
                refreshCategories();
            }
        } catch (RuntimeException failure) {
            UiUtils.handle(panel, "open the category dialog", failure);
        }
    }

    /** Edits the selected category. */
    public void editCategory() {
        Category category = panel.getSelectedCategory();
        if (category == null) {
            UiUtils.info(panel, "Select a category in the table first.");
            return;
        }
        try {
            AdminDialogs.CategoryDialog dialog = new AdminDialogs.CategoryDialog(
                    UiUtils.windowOf(panel), category, data -> context.getCatalogService()
                            .updateCategory(category.getId(), data.name(), data.description()));
            dialog.setVisible(true);
            if (dialog.isSaved()) {
                refreshCategories();
            }
        } catch (RuntimeException failure) {
            UiUtils.handle(panel, "open the category dialog", failure);
        }
    }

    /** Deletes the selected category after confirmation. */
    public void deleteCategory() {
        Category category = panel.getSelectedCategory();
        if (category == null) {
            UiUtils.info(panel, "Select a category in the table first.");
            return;
        }
        if (!UiUtils.confirm(panel, "Delete category '" + category.getName() + "'?\n\n"
                + "The deletion is refused while products still use the category.")) {
            return;
        }
        try {
            context.getCatalogService().deleteCategory(category.getId());
            refreshCategories();
        } catch (InventoryException failure) {
            UiUtils.handle(panel, "delete the category", failure);
        } catch (RuntimeException failure) {
            UiUtils.handle(panel, "delete the category", failure);
        }
    }

    /** Opens the supplier form. */
    public void addSupplier() {
        try {
            AdminDialogs.SupplierDialog dialog = new AdminDialogs.SupplierDialog(
                    UiUtils.windowOf(panel), null, data -> context.getCatalogService()
                            .createSupplier(data.name(), data.contactName(), data.phone(),
                                    data.email()));
            dialog.setVisible(true);
            if (dialog.isSaved()) {
                refreshSuppliers();
            }
        } catch (RuntimeException failure) {
            UiUtils.handle(panel, "open the supplier dialog", failure);
        }
    }

    /** Edits the selected supplier. */
    public void editSupplier() {
        Supplier supplier = panel.getSelectedSupplier();
        if (supplier == null) {
            UiUtils.info(panel, "Select a supplier in the table first.");
            return;
        }
        try {
            AdminDialogs.SupplierDialog dialog = new AdminDialogs.SupplierDialog(
                    UiUtils.windowOf(panel), supplier, data -> context.getCatalogService()
                            .updateSupplier(supplier.getId(), data.name(), data.contactName(),
                                    data.phone(), data.email()));
            dialog.setVisible(true);
            if (dialog.isSaved()) {
                refreshSuppliers();
            }
        } catch (RuntimeException failure) {
            UiUtils.handle(panel, "open the supplier dialog", failure);
        }
    }

    /** Deletes the selected supplier after confirmation. */
    public void deleteSupplier() {
        Supplier supplier = panel.getSelectedSupplier();
        if (supplier == null) {
            UiUtils.info(panel, "Select a supplier in the table first.");
            return;
        }
        if (!UiUtils.confirm(panel, "Delete supplier '" + supplier.getName() + "'?\n\n"
                + "The deletion is refused while products still use the supplier.")) {
            return;
        }
        try {
            context.getCatalogService().deleteSupplier(supplier.getId());
            refreshSuppliers();
        } catch (InventoryException failure) {
            UiUtils.handle(panel, "delete the supplier", failure);
        } catch (RuntimeException failure) {
            UiUtils.handle(panel, "delete the supplier", failure);
        }
    }
}
