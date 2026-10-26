package com.cjlu.inventory.ui.controller;

import com.cjlu.inventory.app.ApplicationContext;
import com.cjlu.inventory.model.User;
import com.cjlu.inventory.ui.LoginDialog;
import com.cjlu.inventory.ui.MainFrame;
import com.cjlu.inventory.ui.util.UiUtils;
import com.cjlu.inventory.ui.view.dialog.ChangePasswordDialog;

import java.util.Objects;

/**
 * Application-level controller for the main window: changing the own password,
 * signing out (dispose, re-open the login dialog, open a fresh main window on
 * success) and the confirmed shutdown of the embedded database.
 */
public final class MainController {

    private final ApplicationContext context;
    private final MainFrame frame;

    public MainController(ApplicationContext context, MainFrame frame) {
        this.context = Objects.requireNonNull(context, "context");
        this.frame = Objects.requireNonNull(frame, "frame");
    }

    /** Opens the change-password dialog for the signed-in user. */
    public void changePassword() {
        User user = context.getAuthService().getCurrentUser();
        if (user == null) {
            UiUtils.warn(frame, "You must be signed in to change your password.");
            return;
        }
        try {
            ChangePasswordDialog dialog = new ChangePasswordDialog(frame, user.getUsername(),
                    data -> context.getAuthService()
                            .changeOwnPassword(data.oldPassword(), data.newPassword()));
            dialog.setVisible(true);
            if (dialog.isSaved()) {
                UiUtils.info(frame, "Your password has been changed. Use the new password the "
                        + "next time you sign in.");
            }
        } catch (RuntimeException failure) {
            UiUtils.handle(frame, "open the change-password dialog", failure);
        }
    }

    /** Confirms and performs logout, re-opening the login dialog. */
    public void signOut() {
        if (!UiUtils.confirm(frame, "Sign out of StockFlow?\n\n"
                + "The current session will be closed. All changes are already saved.")) {
            return;
        }
        frame.deactivateObserver();
        try {
            context.getAuthService().logout();
        } catch (RuntimeException ignored) {
            // Logging out cannot leave inconsistent data; continue with the dialog.
        }
        frame.dispose();

        LoginDialog loginDialog = new LoginDialog(context);
        loginDialog.setVisible(true);
        if (loginDialog.isAuthenticated()) {
            MainFrame nextFrame = new MainFrame(context);
            nextFrame.setVisible(true);
        } else {
            shutdownAndExit();
        }
    }

    /** Confirms and exits the application, shutting the embedded database down. */
    public void exitApplication() {
        if (!UiUtils.confirm(frame, "Exit StockFlow?\n\nThe embedded database will be shut "
                + "down. All changes are already saved.")) {
            return;
        }
        shutdownAndExit();
    }

    /** Shows the about / demo-accounts information. */
    public void showAbout() {
        UiUtils.info(frame, "StockFlow Inventory Management System\n"
                + "Version 2.0.0 - COMP603/ENSE600 Project 2\n\n"
                + "Java 17  -  Swing GUI  -  Apache Derby (embedded)  -  JDBC DAO\n\n"
                + "Demo accounts:\n"
                + "  admin / admin123  (Administrator)\n"
                + "  staff / staff123  (Warehouse Staff)");
    }

    private void shutdownAndExit() {
        frame.deactivateObserver();
        try {
            context.getAuthService().logout();
        } catch (RuntimeException ignored) {
            // Nothing to roll back; the database is shutting down anyway.
        }
        try {
            context.getDatabaseManager().shutdown();
        } catch (RuntimeException shutdownFailure) {
            UiUtils.error(frame, "The database could not be shut down cleanly:\n"
                    + shutdownFailure.getMessage());
        }
        System.exit(0);
    }
}
