package com.cjlu.inventory.ui.view.dialog;

import com.cjlu.inventory.exception.ValidationException;
import com.cjlu.inventory.model.Category;
import com.cjlu.inventory.model.Supplier;
import com.cjlu.inventory.ui.util.UiUtils;

import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Window;

/**
 * Container for the small master-data and user-account dialogs of the
 * Administration tab. Keeping the simple forms together avoids a swarm of
 * near-empty files while each dialog still owns its own validation.
 */
public final class AdminDialogs {

    private AdminDialogs() {
        // utility container
    }

    /** Create / edit a product category. */
    public static final class CategoryDialog extends BaseDialog {

        private static final long serialVersionUID = 1L;

        /** Typed category values collected by the dialog. */
        public record CategoryData(String name, String description) {
        }

        private final boolean editMode;
        private final JTextField nameField = new JTextField(26);
        private final JTextField descriptionField = new JTextField(26);

        public CategoryDialog(Window owner, Category existing, DialogHandler<CategoryData> handler) {
            super(owner, existing == null ? "New Category" : "Edit Category");
            this.editMode = existing != null;
            if (existing != null) {
                nameField.setText(existing.getName());
                descriptionField.setText(existing.getDescription());
            }
            JPanel content = new JPanel(new BorderLayout(0, 10));
            content.setOpaque(false);
            JPanel form = UiUtils.formPanel();
            UiUtils.addFormRow(form, "Category Name *", nameField);
            UiUtils.addFormRow(form, "Description (optional)", descriptionField);
            content.add(form, BorderLayout.CENTER);
            content.add(UiUtils.subtitleLabel("Categories group products and drive the valuation chart."),
                    BorderLayout.SOUTH);
            initialize(content, editMode ? "Save Changes" : "Create Category",
                    () -> execute(editMode ? "update the category" : "create the category",
                            () -> handler.save(new CategoryData(nameField.getText(),
                                    descriptionField.getText()))));
            centerOnOwner();
        }
    }

    /** Create / edit a supplier. */
    public static final class SupplierDialog extends BaseDialog {

        private static final long serialVersionUID = 1L;

        /** Typed supplier values collected by the dialog. */
        public record SupplierData(String name, String contactName, String phone, String email) {
        }

        private final boolean editMode;
        private final JTextField nameField = new JTextField(26);
        private final JTextField contactField = new JTextField(26);
        private final JTextField phoneField = new JTextField(26);
        private final JTextField emailField = new JTextField(26);

        public SupplierDialog(Window owner, Supplier existing, DialogHandler<SupplierData> handler) {
            super(owner, existing == null ? "New Supplier" : "Edit Supplier");
            this.editMode = existing != null;
            if (existing != null) {
                nameField.setText(existing.getName());
                contactField.setText(existing.getContactName());
                phoneField.setText(existing.getPhone());
                emailField.setText(existing.getEmail());
            }
            JPanel content = new JPanel(new BorderLayout(0, 10));
            content.setOpaque(false);
            JPanel form = UiUtils.formPanel();
            UiUtils.addFormRow(form, "Supplier Name *", nameField);
            UiUtils.addFormRow(form, "Contact Name *", contactField);
            UiUtils.addFormRow(form, "Phone *", phoneField);
            UiUtils.addFormRow(form, "E-mail *", emailField);
            content.add(form, BorderLayout.CENTER);
            content.add(UiUtils.subtitleLabel(
                            "A supplier can only be deleted while no product references it."),
                    BorderLayout.SOUTH);
            initialize(content, editMode ? "Save Changes" : "Create Supplier",
                    () -> execute(editMode ? "update the supplier" : "create the supplier",
                            () -> handler.save(new SupplierData(nameField.getText(),
                                    contactField.getText(), phoneField.getText(),
                                    emailField.getText()))));
            centerOnOwner();
        }
    }

    /** Create a new warehouse-staff account (administrator only). */
    public static final class CreateStaffDialog extends BaseDialog {

        private static final long serialVersionUID = 1L;

        /** Typed account values collected by the dialog. */
        public record StaffData(String username, String displayName, String password) {
        }

        private final JTextField usernameField = new JTextField(22);
        private final JTextField displayNameField = new JTextField(22);
        private final JPasswordField passwordField = new JPasswordField(22);
        private final JPasswordField confirmField = new JPasswordField(22);

        public CreateStaffDialog(Window owner, DialogHandler<StaffData> handler) {
            super(owner, "Create Staff Account");
            JPanel content = new JPanel(new BorderLayout(0, 10));
            content.setOpaque(false);
            JPanel form = UiUtils.formPanel();
            UiUtils.addFormRow(form, "Username *", usernameField);
            UiUtils.addFormRow(form, "Display Name *", displayNameField);
            UiUtils.addFormRow(form, "Password *", passwordField);
            UiUtils.addFormRow(form, "Confirm Password *", confirmField);
            content.add(form, BorderLayout.CENTER);
            content.add(UiUtils.subtitleLabel(
                            "Usernames use lower-case letters, digits or '_'. Passwords need at least 6 characters."),
                    BorderLayout.SOUTH);
            initialize(content, "Create Account", () -> execute("create the staff account", () -> {
                String password = new String(passwordField.getPassword());
                String confirmed = new String(confirmField.getPassword());
                if (!password.equals(confirmed)) {
                    throw new ValidationException("The password and its confirmation do not match.");
                }
                handler.save(new StaffData(usernameField.getText(), displayNameField.getText(),
                        password));
                passwordField.setText("");
                confirmField.setText("");
            }));
            centerOnOwner();
        }
    }

    /** Reset the password of an existing account (administrator only). */
    public static final class ResetPasswordDialog extends BaseDialog {

        private static final long serialVersionUID = 1L;

        private final JPasswordField passwordField = new JPasswordField(22);
        private final JPasswordField confirmField = new JPasswordField(22);

        public ResetPasswordDialog(Window owner, String username, DialogHandler<String> handler) {
            super(owner, "Reset Password");
            JPanel content = new JPanel(new BorderLayout(0, 10));
            content.setOpaque(false);
            JPanel form = UiUtils.formPanel();
            UiUtils.addFormRow(form, "Account", UiUtils.mutedLabel(username));
            UiUtils.addFormRow(form, "New Password *", passwordField);
            UiUtils.addFormRow(form, "Confirm Password *", confirmField);
            content.add(form, BorderLayout.CENTER);
            content.add(UiUtils.subtitleLabel(
                            "The account keeps its role; only the password is replaced."),
                    BorderLayout.SOUTH);
            initialize(content, "Reset Password", () -> execute("reset the password", () -> {
                String password = new String(passwordField.getPassword());
                String confirmed = new String(confirmField.getPassword());
                if (!password.equals(confirmed)) {
                    throw new ValidationException("The password and its confirmation do not match.");
                }
                handler.save(password);
                passwordField.setText("");
                confirmField.setText("");
            }));
            centerOnOwner();
        }
    }
}
