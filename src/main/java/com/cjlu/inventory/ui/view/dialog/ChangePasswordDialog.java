package com.cjlu.inventory.ui.view.dialog;

import com.cjlu.inventory.exception.ValidationException;

import javax.swing.JPanel;
import javax.swing.JPasswordField;
import java.awt.BorderLayout;
import java.awt.Window;

/**
 * Changes the password of the signed-in user. The current password is verified
 * by the authentication service; the dialog only checks that the two new
 * password fields match.
 */
public class ChangePasswordDialog extends BaseDialog {

    private static final long serialVersionUID = 1L;

    /** Typed password values collected by the dialog. */
    public record PasswordData(String oldPassword, String newPassword) {
    }

    private final JPasswordField oldPasswordField = new JPasswordField(22);
    private final JPasswordField newPasswordField = new JPasswordField(22);
    private final JPasswordField confirmPasswordField = new JPasswordField(22);

    public ChangePasswordDialog(Window owner, String username,
                                DialogHandler<PasswordData> handler) {
        super(owner, "Change Password");
        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setOpaque(false);
        JPanel form = com.cjlu.inventory.ui.util.UiUtils.formPanel();
        com.cjlu.inventory.ui.util.UiUtils.addFormRow(form, "Account",
                com.cjlu.inventory.ui.util.UiUtils.mutedLabel(username));
        com.cjlu.inventory.ui.util.UiUtils.addFormRow(form, "Current Password *", oldPasswordField);
        com.cjlu.inventory.ui.util.UiUtils.addFormRow(form, "New Password *", newPasswordField);
        com.cjlu.inventory.ui.util.UiUtils.addFormRow(form, "Confirm New Password *", confirmPasswordField);
        content.add(form, BorderLayout.CENTER);
        content.add(com.cjlu.inventory.ui.util.UiUtils.subtitleLabel(
                "The new password must be at least 6 and at most 64 characters long."),
                BorderLayout.SOUTH);

        initialize(content, "Change Password", () -> execute("change the password", () -> {
            String oldPassword = new String(oldPasswordField.getPassword());
            String newPassword = new String(newPasswordField.getPassword());
            String confirmed = new String(confirmPasswordField.getPassword());
            if (oldPassword.isBlank()) {
                throw new ValidationException("Current password is required.");
            }
            if (newPassword.isBlank()) {
                throw new ValidationException("New password is required.");
            }
            if (!newPassword.equals(confirmed)) {
                throw new ValidationException("The new password and its confirmation do not match.");
            }
            handler.save(new PasswordData(oldPassword, newPassword));
            oldPasswordField.setText("");
            newPasswordField.setText("");
            confirmPasswordField.setText("");
        }));
        centerOnOwner();
    }
}
