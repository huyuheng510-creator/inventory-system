package com.cjlu.inventory.ui;

import com.cjlu.inventory.app.ApplicationContext;
import com.cjlu.inventory.ui.controller.LoginController;
import com.cjlu.inventory.ui.util.Theme;
import com.cjlu.inventory.ui.util.UiUtils;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Optional;

/**
 * Modal sign-in dialog, the first window of the application.
 *
 * <p>The dialog is authenticated through {@code AuthService} (delegated to
 * {@link LoginController}); wrong credentials show a red inline error and let
 * the user try again. Closing the dialog or pressing Exit leaves
 * {@link #isAuthenticated()} {@code false}, which makes the application exit
 * without opening the main window.</p>
 */
public class LoginDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final ApplicationContext context;
    private final LoginController controller;
    private final JTextField usernameField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JLabel errorLabel = new JLabel(" ");
    private JButton signInButton;
    private boolean authenticated;

    public LoginDialog(ApplicationContext context) {
        super((Frame) null, "StockFlow - Sign In", true);
        Theme.install();
        this.context = context;
        this.controller = new LoginController(context);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        buildUi();
        setSize(new Dimension(470, 540));
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent event) {
                usernameField.requestFocusInWindow();
            }
        });
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BACKGROUND);
        root.setOpaque(true);

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildBody(), BorderLayout.CENTER);
        setContentPane(root);
        getRootPane().setDefaultButton(signInButton);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D g2 = (Graphics2D) graphics.create();
                try {
                    g2.setPaint(new GradientPaint(0, 0, Theme.PRIMARY,
                            getWidth(), getHeight(), Theme.PRIMARY_LIGHT));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                } finally {
                    g2.dispose();
                }
            }
        };
        header.setOpaque(true);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(BorderFactory.createEmptyBorder(28, 34, 24, 34));

        JLabel appName = new JLabel("StockFlow");
        appName.setFont(Theme.FONT_BASE.deriveFont(Font.BOLD, 30f));
        appName.setForeground(Color.WHITE);
        appName.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Inventory Management System - Project 2");
        subtitle.setFont(Theme.FONT_BASE.deriveFont(Font.PLAIN, 13.5f));
        subtitle.setForeground(new Color(0xDCE7F2));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel tech = new JLabel("Swing GUI  -  Apache Derby (embedded)  -  JDBC DAO");
        tech.setFont(Theme.FONT_SMALL);
        tech.setForeground(new Color(0xB9CCE0));
        tech.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(appName);
        header.add(Box.createVerticalStrut(4));
        header.add(subtitle);
        header.add(Box.createVerticalStrut(6));
        header.add(tech);
        return header;
    }

    private JPanel buildBody() {
        JPanel body = new JPanel();
        body.setOpaque(true);
        body.setBackground(Theme.BACKGROUND);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(22, 34, 20, 34));

        JLabel formTitle = new JLabel("Sign in to continue");
        formTitle.setFont(Theme.FONT_SECTION);
        formTitle.setForeground(Theme.TEXT);
        formTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(formTitle);
        body.add(Box.createVerticalStrut(14));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        UiUtils.addFormRow(form, "Username", usernameField);
        UiUtils.addFormRow(form, "Password", passwordField);
        body.add(form);
        body.add(Box.createVerticalStrut(8));

        errorLabel.setFont(Theme.FONT_SMALL_BOLD);
        errorLabel.setForeground(Theme.DANGER);
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        errorLabel.setText(" ");
        body.add(errorLabel);
        body.add(Box.createVerticalStrut(8));

        signInButton = UiUtils.primaryButton("Sign In");
        signInButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        signInButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        signInButton.addActionListener(action -> attemptLogin());
        body.add(signInButton);
        body.add(Box.createVerticalStrut(16));

        JPanel demoPanel = new JPanel();
        demoPanel.setOpaque(false);
        demoPanel.setLayout(new BoxLayout(demoPanel, BoxLayout.Y_AXIS));
        demoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel demoTitle = UiUtils.subtitleLabel("Demo accounts:");
        demoTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel adminDemo = UiUtils.mutedLabel("admin / admin123     (Administrator)");
        adminDemo.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel staffDemo = UiUtils.mutedLabel("staff / staff123     (Warehouse Staff)");
        staffDemo.setAlignmentX(Component.LEFT_ALIGNMENT);
        demoPanel.add(demoTitle);
        demoPanel.add(Box.createVerticalStrut(2));
        demoPanel.add(adminDemo);
        demoPanel.add(staffDemo);
        body.add(demoPanel);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        bottom.setOpaque(false);
        JButton exitButton = UiUtils.button("Exit");
        exitButton.addActionListener(action -> dispose());
        bottom.add(exitButton);
        body.add(Box.createVerticalStrut(10));
        body.add(bottom);
        return body;
    }

    private void attemptLogin() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        errorLabel.setText(" ");
        signInButton.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            Optional<String> failure = controller.login(username, password);
            if (failure.isPresent()) {
                errorLabel.setText(failure.get());
                passwordField.setText("");
                passwordField.requestFocusInWindow();
            } else {
                authenticated = true;
                SwingUtilities.invokeLater(this::dispose);
            }
        } finally {
            signInButton.setEnabled(true);
            setCursor(Cursor.getDefaultCursor());
        }
    }

    /** True when the user signed in successfully. */
    public boolean isAuthenticated() {
        return authenticated;
    }

    /** The application context the dialog was created with. */
    public ApplicationContext getContext() {
        return context;
    }
}
