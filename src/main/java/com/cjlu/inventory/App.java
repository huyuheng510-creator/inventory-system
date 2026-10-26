package com.cjlu.inventory;

import com.cjlu.inventory.app.ApplicationContext;
import com.cjlu.inventory.ui.LoginDialog;
import com.cjlu.inventory.ui.MainFrame;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Font;

/**
 * Application entry point for the Project 2 Swing desktop application.
 *
 * <p>Starts the embedded Derby database, shows the modal login dialog and,
 * after a successful sign-in, opens the main window. The database is created
 * and seeded automatically on the first run, so no manual configuration is
 * required.</p>
 */
public final class App {

    private App() {
        // entry point
    }

    public static void main(String[] args) {
        installLookAndFeel();
        SwingUtilities.invokeLater(App::start);
    }

    private static void start() {
        ApplicationContext context;
        try {
            context = ApplicationContext.createDefault();
        } catch (RuntimeException startFailure) {
            JOptionPane.showMessageDialog(null,
                    "The inventory database could not be started.\n\n"
                            + startFailure.getMessage(),
                    "StockFlow - Startup Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        LoginDialog loginDialog = new LoginDialog(context);
        loginDialog.setVisible(true);
        if (!loginDialog.isAuthenticated()) {
            context.getDatabaseManager().shutdown();
            System.exit(0);
        }

        try {
            MainFrame mainFrame = new MainFrame(context);
            mainFrame.setVisible(true);
        } catch (RuntimeException guiFailure) {
            JOptionPane.showMessageDialog(null,
                    "The main window could not be opened.\n\n" + guiFailure.getMessage(),
                    "StockFlow - Startup Error", JOptionPane.ERROR_MESSAGE);
            context.getDatabaseManager().shutdown();
            System.exit(1);
        }
    }

    /** Chooses Nimbus when available and applies a consistent base font. */
    private static void installLookAndFeel() {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception ignored) {
            // Fall back to the platform look and feel.
        }
        Font baseFont = new Font("Segoe UI", Font.PLAIN, 13);
        UIManager.put("Label.font", baseFont);
        UIManager.put("Button.font", baseFont);
        UIManager.put("Table.font", baseFont);
        UIManager.put("TableHeader.font", baseFont.deriveFont(Font.BOLD));
        UIManager.put("TextField.font", baseFont);
        UIManager.put("ComboBox.font", baseFont);
        UIManager.put("TabbedPane.font", baseFont.deriveFont(Font.BOLD, 13f));
    }
}
